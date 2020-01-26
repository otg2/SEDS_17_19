-module(node3).
-export([start/1, start/2]).

-define(Stabilize, 500).
-define(Timeout, 10000).


start(Id) ->
  start(Id, nil).

start(Id, Peer) ->
  %timer:start(),
  spawn(fun() -> init(Id, Peer) end).

init(Id, Peer) ->
  Predecessor = nil,
  {ok, Successor} = connect(Id, Peer),
  schedule_stabilize(),
  node(Id, Predecessor, Successor, nil, storage:create()).

connect(Id, nil) ->
  {ok, {Id, nil, self()}};
connect(_Id, Peer) ->
%http://erlang.org/doc/man/erlang.html#make_ref-0
%Returns a unique reference. The reference is unique among connected nodes.
  Qref = make_ref(),
  Peer ! {key, Qref, self()},
  io:format("Key connect ~w~n",[{key, Qref, Peer}]),
  receive
    {Qref, Skey} ->
      %io:format("receive ~w~n",[{Qref, Skey}]),
      %io:format("ok ~w~n",[{Skey, Peer}]),
      Sref = monitor(Peer),
      {ok, {Skey, Sref, Peer}}
    after ?Timeout ->
      io:format("Time out: no response~n",[])
  end.

node(Id, Predecessor, Successor, Next, Store) ->
  receive
    {key, Qref, Peer} ->
      %io:format("Key connect ~w~n",[{key, Qref, Peer}]),
      Peer ! {Qref, Id},
      node(Id, Predecessor, Successor, Next, Store);
    {notify, New} ->
      {Pred, SplitStore} = notify(New, Id, Predecessor, Store),
      %Pred = notify(New, Id, Predecessor, Store),
      node(Id, Pred, Successor, Next, SplitStore);
    {request, Peer} ->
      % 1 io:format("start request ~w~n",[Id]),
      request(Peer, Predecessor, Successor), % next
      node(Id, Predecessor, Successor, Next, Store);
    stabilize ->
      % 2 io:format("start stabilize ~w~n",[Id]),
      stabilize(Successor),
      node(Id, Predecessor, Successor, Next, Store);
    {status, Pred, Nexter} ->
       %io:format("start status ~w~n",[Id]),
      {Succ, NewNext} = stabilize(Pred, Nexter, Id, Successor),
       %io:format("Status Id ~w~n Next ~w~n~n",[Id, NewNext]),
      node(Id, Predecessor, Succ, NewNext, Store);
    {add, Key, Value, Qref, Client} ->
      %io:format("Add ~w~n", [{add, Key, Value, Qref, Client}]),
      Added = add(Key, Value, Qref, Client, Id, Predecessor, Successor, Store),
      node(Id, Predecessor, Successor, Next, Added);
    {lookup, Key, Qref, Client} ->
      lookup(Key, Qref, Client, Id, Predecessor, Successor, Store),
      node(Id, Predecessor, Successor, Next, Store);
    {handover, Elements} ->
      Merged = storage:merge(Store, Elements),
      node(Id, Predecessor, Successor, Next, Merged);

    probe ->
      %io:format("succ ~n", []),
      create_probe(Id, Successor),
      node(Id, Predecessor, Successor, Next, Store);
    {probe, Id, Nodes, T} ->
      remove_probe(T, Nodes),
      node(Id, Predecessor, Successor, Next, Store);
    {probe, Ref, Nodes, T} ->
      %io:format("probe tuple ~w~n", [{probe, Ref, Nodes, T}]),
      forward_probe(Ref, T, Nodes, Id, Successor),
      node(Id, Predecessor, Successor, Next, Store);
    {'DOWN', Ref, process, _, _} ->

      io:format("node ~w~n detect down ~w~n ~n~w~n~w~n~w~n", [Id, Ref, Predecessor, Successor, Next]),
      {Pred, Succ, Nxt} = down(Ref, Predecessor, Successor, Next),
      node(Id, Pred, Succ, Nxt, Store);
    {simulateDown, Ref } ->
      {Pred, Succ, Nxt} = down(Ref, Predecessor, Successor, Next),
      node(Id, Pred, Succ, Nxt, Store);
    info ->
      io:format("node info ~n~w~n~w~n~w~n~w~n", [Id, Predecessor, Successor, Next]),
      node(Id, Predecessor, Successor, Next, Store);

    stop ->
      ok
  end.

add(Key, Value, Qref, Client, Id, {Pkey, _, _}, {_, _, Spid}, Store) ->
  case key:between(Key, Pkey, Id) of
    true ->
      Client ! {Qref, ok},
      storage:add(Key, Value, Store);
    false ->
      Spid ! {add, Key, Value, Qref, Client},
      Store % return the store itself
  end.

lookup(Key, Qref, Client, Id, {Pkey, _, _}, Successor, Store) ->
  case key:between(Key, Pkey, Id) of
    true ->
      Result = storage:lookup(Key, Store),
      Client ! {Qref, Result};
    false ->
      {_, _, Spid} = Successor,
      Spid ! {lookup, Key, Qref, Client}
  end.

create_probe(Id, Successor) ->
  {_, _, Pid} = Successor, %% CHANGE HERE

  io:format("create ~w~n", [Id]),

  Pid ! {probe, Id, [Id], erlang:now()}.

remove_probe(Time, Nodes) ->
  DiffTime = timer:now_diff(now(), Time), % Calculates the time difference

  io:format("remove time ~w, list ~w~n", [ DiffTime, Nodes]).

forward_probe(Ref, Time, Nodes, Id, Successor) ->
  {_, _, Pid} = Successor, %% AND HERE

  io:format("probe forward tuple ~w  ~n", [Pid ]),

  Pid ! {probe, Ref, Nodes ++ [Id], Time}.

notify({Nkey, Npid}, Id, Predecessor, Store) ->
  case Predecessor of
    nil -> % case closed, he already is Predecessor
      Keep = handover(Id, Store, Nkey, Npid),
      Nref = monitor(Npid),
      {{Nkey, Nref, Npid}, Keep};
    {Pkey, Pref,  _} ->
      case key:between(Nkey, Pkey, Id) of
        true -> % should be Predecessor
          Keep = handover(Id, Store, Nkey, Npid),
          Nref = monitor(Npid),
          drop(Pref),
          {{Nkey, Nref, Npid}, Keep};
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

request(Peer, Predecessor, Successor) ->
  %io:format("request: ~w ~n", [{Peer, Predecessor, Next}]),
  case Predecessor of
    nil ->
      Peer ! {status, nil, Successor};
    {Pkey, _, Ppid} ->
      %io:format("node ~w~n: Keep: ~w~n Rest ~w~n", [Id, Keep, Rest]),
      Peer ! {status, {Pkey, Ppid}, Successor}
  end.

schedule_stabilize() ->
  timer:send_interval(?Stabilize, self(), stabilize).

stabilize({_, _, Spid}) ->
  Spid ! {request, self()}.

stabilize(Pred, Next, Id, Successor) ->
  %io:format(" basic stabilize ~w~n", [Pred]),
  {Skey, Sref, Spid} = Successor,
  case Pred of
    nil -> %todo - notify successor that we exist
      Spid ! {notify, {Id, self()}},
      {Successor, Next};
    {Id, _} -> %todo - do nothing (back to us)
      {Successor, Next};
    {Skey, _} -> %todo - to itself, notify again that we exist
      Spid ! {notify, {Id, self()}},
      {Successor, Next};
    {Xkey, Xpid} -> %todo: aonther node
      %io:format(" Xkey ~w Xpid ~w ~n", [ Xkey, Xpid]),
      case key:between(Xkey, Id, Skey) of
        true -> % request stabilization
          Xpid ! {request, self()},
          drop(Sref),
          Xref = monitor(Xpid),
          {{Xkey, Xref, Xpid}, {Skey, Spid} }; %%% THIS WAS WRONG¨!!!
        false ->
          Spid ! {notify, {Id, self()}},
          {Successor, Next}
      end
  end.

monitor(Pid) ->
  erlang:monitor(process, Pid).
drop(nil) ->
  ok;
drop(Pid) ->
  erlang:demonitor(Pid, [flush]).
% down(Ref, Predecessor, Successor, Next),
% -> to
% {Pred, Succ, Nxt}
down(Ref, {_, Ref, _}, Successor, Next) ->
	io:format("Down 1"),
  io:format("node down 1, ~w~n~w~n~w~n~n", [Ref,Successor, Next]),

	{nil, Successor, Next};
%% successor down
down(Ref, Predecessor, {_, Ref, _}, {Nkey, Nref, Npid}) ->
	io:format("Down 2"),
	self() ! stabilize,
	{Predecessor, {Nkey, monitor(Npid), Npid}, nil}.

down2(Ref, {_, Ref, _}, Successor, Next) ->
  io:format("node down 1, ~w~n", [{Successor, Next}]),
  {nil, Successor, Next};
down2(Ref, Predecessor, {_, Ref, _}, {Nkey, Npid}) ->
  io:format("node down 2, ~w~n", [{Predecessor, {Nkey, Npid}}]),
  self() ! stabilize,
  Nref = monitor(Npid),
  {Predecessor, {Nkey, Nref, Npid}, nil}.
