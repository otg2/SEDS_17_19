-module(node2).
-export([start/1, start/2]).

-define(Stabilize, 500).
-define(Timeout, 10000).


start(Id) ->
  start(Id, nil).

start(Id, Peer) ->
  timer:start(),
  spawn(fun() -> init(Id, Peer) end).

init(Id, Peer) ->
  Predecessor = nil,
  {ok, Successor} = connect(Id, Peer),
  %io:format("init after connect ~w~n",[{Id, Peer}]),
  schedule_stabilize(),
  node(Id, Predecessor, Successor, storage:create()).

connect(Id, nil) ->
  {ok, {Id,self()}};
connect(_Id, Peer) ->
%http://erlang.org/doc/man/erlang.html#make_ref-0
  %Returns a unique reference. The reference is unique among connected nodes.
  Qref = make_ref(),
  Peer ! {key, Qref, self()},
  %io:format("Key connect ~w~n",[{key, Qref, Peer}]),
  receive
    {Qref, Skey} ->
      %io:format("receive ~w~n",[{Qref, Skey}]),
      %io:format("ok ~w~n",[{Skey, Peer}]),
      {ok, {Skey, Peer}}
  after ?Timeout ->
    io:format("Time out: no repsonse from   ~n",[])
  end.

node(Id, Predecessor, Successor, Store) ->
  receive
    {key, Qref, Peer} ->
      %io:format("Key connect ~w~n",[{key, Qref, Peer}]),
      Peer ! {Qref, Id},
      node(Id, Predecessor, Successor, Store);
    {notify, New} ->
      {Pred, SplitStore} = notify(New, Id, Predecessor, Store),
      %Pred = notify(New, Id, Predecessor, Store),
      node(Id, Pred, Successor, SplitStore);
    {request, Peer} ->
      % 1 io:format("start request ~w~n",[Id]),

      request(Peer, Predecessor),
      node(Id, Predecessor, Successor, Store);
    stabilize ->
      % 2 io:format("start stabilize ~w~n",[Id]),
      stabilize(Successor),
      node(Id, Predecessor, Successor , Store);
    {status, Pred} ->
      % 3 io:format("start status ~w~n",[Id]),
      Succ = stabilize(Pred, Id, Successor),
      node(Id, Predecessor, Succ, Store);
    {add, Key, Value, Qref, Client} ->
      %io:format("Add ~w~n", [{add, Key, Value, Qref, Client}]),
      Added = add(Key, Value, Qref, Client, Id, Predecessor, Successor, Store),
      node(Id, Predecessor, Successor, Added);
    {lookup, Key, Qref, Client} ->
      lookup(Key, Qref, Client, Id, Predecessor, Successor, Store),
      node(Id, Predecessor, Successor, Store);
    {handover, Elements} ->
      Merged = storage:merge(Store, Elements),
      node(Id, Predecessor, Successor, Merged);
    probe ->
      %io:format("succ ~n", []),
      OrgTime = erlang:now(),
      create_probe(Id, Successor, Store, OrgTime),
      node(Id, Predecessor, Successor, Store);
    {probe, Id, Nodes, T} ->
      remove_probe(T, Nodes),
      node(Id, Predecessor, Successor, Store);
    {probe, Ref, Nodes, T} ->
      %io:format("probe tuple ~w~n", [{probe, Ref, Nodes, T}]),

      forward_probe(Ref, T, Nodes, Id, Successor, Store),
      node(Id, Predecessor, Successor, Store);
    stop ->
      stop

  end.


add(Key, Value, Qref, Client, Id, {Pkey, _}, {Skey, Spid}, Store) ->
  case key:between(Key, Pkey, Id) of % see if key is between pred and itself.
    true ->
      Client ! {Qref, ok},
      storage:add(Key, Value, Store);
    false ->
      Spid ! { add, Key, Value, Qref, Client},
      Store % return the store itself
  end.

lookup(Key, Qref, Client, Id, {Pkey, _}, Successor, Store) ->
  case key:between(Key, Pkey, Id)  of
    true ->
      Result = storage:lookup(Key, Store),
      Client ! {Qref, Result};
    false ->
      {_, Spid} = Successor,
      Spid ! {lookup, Key, Qref, Client}
  end.

create_probe(Id, Successor, Store, OrgTime) ->
    {_Pkey, Pid} = Successor,

    io:format("succ ~w store ~w~n", [Pid, length(Store)]),
    
    Pid ! {probe, Id, [Id], OrgTime}.

remove_probe(Time, Nodes) ->
    DiffTime = timer:now_diff(now(), Time), % Calculates the time difference

    io:format(" time ~w, list ~w~n", [ DiffTime, Nodes]).

forward_probe(Ref, Time, Nodes, Id, Successor, Store) ->
    {_Pkey, Pid} = Successor,

    io:format("probe tuple ~w store ~w time ~w~n", [Pid, length(Store),Time]),

    Pid ! {probe, Ref, Nodes ++ [Id], Time}.


notify({Nkey, Npid}, Id, Predecessor, Store) ->
  case Predecessor of
    nil -> % case closed, he already is Predecessor
      Keep = handover(Id, Store, Nkey, Npid),
      {{Nkey, Npid}, Keep};
    {Pkey, _} ->
      case key:between(Nkey, Pkey, Id) of
        true -> % should be Predecessor
          Keep = handover(Id, Store, Nkey, Npid),
          {{Nkey, Npid}, Keep};
        false -> % should not be, return old Predecessor AND store
          {Predecessor, Store}
      end
    end.

handover(Id, Store, Nkey, Npid) ->
  % this needed to be fixed
  {Keep, Rest} = storage:split(Nkey, Id, Store),
  %io:format("node ~w~n: Keep: ~w~n Rest ~w~n", [Id, Keep, Rest]),
  Npid ! {handover, Rest},
  Keep.


request(Peer, Predecessor) ->
  case Predecessor of
    nil ->
      Peer ! {status, nil};
    {Pkey, Ppid} ->
      Peer ! {status, {Pkey, Ppid}}
  end.

% call when node is created
schedule_stabilize() ->
  timer:send_interval(?Stabilize, self(), stabilize).

stabilize ({_, Spid}) ->
  %io:format(" basic stabilize ~w~n", [Spid]),
  Spid ! {request, self()}.

% pred of my Successor
stabilize(Pred, Id, Successor) ->
  %io:format(" Pred ~w~n", [ Pred]),
  {Skey, Spid} = Successor,
  case Pred of
    nil -> %todo - notify successor that we exist
      Spid ! {notify, {Id, self()}},
      Successor;
    {Id, _} -> %todo - do nothing (back to us)
      Successor;
    {Skey, _} -> %todo - to itself, notify again that we exist
      Spid ! {notify, {Id, self()}},
      Successor;
    {Xkey, Xpid} -> %todo: aonther node
      %io:format(" Xkey ~w Xpid ~w ~n", [ Xkey, Xpid]),
      case key:between(Xkey, Id, Skey) of
        true -> % request stabilization
          Xpid ! {request, self()},
          Pred;
        false ->
          Spid ! {notify, {Id, self()}},
          Successor
        end
  end.
