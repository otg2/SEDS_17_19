-module(node1).
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
  schedule_stabilize(),
  node(Id, Predecessor, Successor).

connect(Id, nil) ->
  {ok, {Id,self()}};
connect(Id, Peer) ->
%http://erlang.org/doc/man/erlang.html#make_ref-0
  %Returns a unique reference. The reference is unique among connected nodes.
  Qref = make_ref(),
  Peer ! {key, Qref, self()},
  receive
    {Qref, Skey} ->
      {ok, {Skey, Peer}}
  after ?Timeout ->
    io:format("Time out: no repsonse from ~w~n",[Id])
  end.

node(Id, Predecessor, Successor) ->
  receive
    {key, Qref, Peer} ->
      Peer ! {Qref, Id},
      node(Id, Predecessor, Successor);
    {notify, New} ->
      Pred = notify(New, Id, Predecessor),
      node(Id, Pred, Successor);
    {request, Peer} ->
      request(Peer, Predecessor),
      node(Id, Predecessor, Successor);
    stabilize ->
      stabilize(Successor),
      node(Id, Predecessor, Successor);
    {status, Pred} ->
      Succ = stabilize(Pred, Id, Successor),
      node(Id, Predecessor, Succ);
    probe ->
      create_probe(Id, Successor),
      node(Id, Predecessor, Successor);
    {probe, Id, Nodes, T} ->
      io:format(" before remove ~w~n", [{probe, Id, Nodes, T}]),
      io:format(" insert remove ~w~n", [{T, Nodes}]),
      remove_probe(T, Nodes),
      node(Id, Predecessor, Successor);
    {probe, Ref, Nodes, T} ->
      io:format("before forward tuple ~w ~n", [{probe, Ref, Nodes, T} ]),
      io:format("insert forward ~w ~n", [{Ref, T, Nodes, Id, Successor}]),

      forward_probe(Ref, T, Nodes, Id, Successor),
      node(Id, Predecessor, Successor)
  end.

create_probe(Id, Successor) ->
    {_Pkey, Pid} = Successor,

    io:format("create probe ~w  time ~w~n", [Pid, erlang:now()]),

    Pid ! {probe, Id, [Id], erlang:now()}.

remove_probe(Time, Nodes) ->
    DiffTime = timer:now_diff(now(), Time), % Calculates the time difference
    io:format(" time ~w, list ~w~n", [ DiffTime, Nodes]).

forward_probe(Ref, Time, Nodes, Id, Successor) ->
    {_Pkey, Pid} = Successor,

    io:format("probe tuple ~w  time ~w~n", [Pid, Time]),

    Pid ! {probe, Ref, Nodes ++ [Id], Time}.

% new is id, self()
notify({Nkey, Npid}, Id, Predecessor) ->
  case Predecessor of
    nil -> % case closed, he already is Predecessor
      {Nkey, Npid};%
    {Pkey, _} ->
      case key:between(Nkey, Pkey, Id) of
        true -> % should be Predecessor
          {Nkey, Npid};
        false -> % should not be, return old Predecessor
          Predecessor
      end
    end.



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
  Spid ! {request, self()}.

stabilize(Pred, Id, Successor) ->
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
      case key:between(Xkey, Id, Skey) of
      true -> % request stabilization
        Xpid ! {request, self()},
        Pred;
      false ->
        Spid ! {notify, {Id, self()}},
        Successor
      end
  end.
