-module(gsm4).
-export([start/1, start/2]).

% ÞETTA ER MEÐ RECEIVED
%-define(CODE, "Z00887").
-define(timeout, 2000).
-define(arghhh, 200).

%master
start(Id) ->
  Rnd = random:uniform(1000),
  Self = self(),
  {ok, spawn_link(fun() -> initleader(Id, Rnd, Self) end)}.

initleader(Id, Rnd, Master) ->
  Received = [],
  random:seed(Rnd, Rnd, Rnd),
  io:format("leaderinit ~w~n",[Master]),

  leader(Id,Master, [], 0, [Master], Received).

%slave
start(Id, Grp) ->
  Rnd = random:uniform(1000),
  Self = self(),
  Received = [],
  {ok, spawn_link(fun() -> initslave(Id, Grp, Rnd, Self) end)}.

initslave(Id, Grp, Rnd, Master) ->
  Received = [],
  random:seed(Rnd, Rnd, Rnd),
  Self = self(),
  Grp ! {join, Master, Self},
  receive
    {view, N, [Leader | Slaves], Group} ->
      %erlang:monitor(process, Leader),
      CurrentView = {view, N, [Leader | Slaves], Group},
      io:format("slaveinit ~w~n",[CurrentView]),
      Master ! {view, Group},
      erlang:monitor(process, Leader),
      slave(Id, Master, Leader, N+1, CurrentView, Slaves, Group, Received) % send last view
    after ?timeout ->
        Master ! {error, "no reply from leader"}
  end.

election(Id, Master, N, Last, Slaves, [_|Group], Received) ->
  Self = self(),
  case Slaves of
    [Self|Rest] ->
      bcast(Id, Last, Rest), % the new leaders last message
      %io:format("last ~w~n",[Last]),
      NewMessage = {view, N, Slaves, Group}, % the new view
      %io:format("newview ~w~n",[NewMessage]),
      bcast(Id, NewMessage , Rest),
      Master ! {view, Group},
      leader(Id, Master, Rest, N+1, Group, Received);
    [Leader|Rest] ->
      erlang:monitor(process, Leader),
      %io:format("leader ~w~n",[Last]),
      slave(Id, Master, Leader, N+1, Last, Rest, Group, Received)
  end.

leader(Id, Master, Slaves, N, Group, Received) ->
  receive
    {mcast, Msg} ->
      MultiMessage = {msg, N, Msg},
      bcast(Id, MultiMessage, Slaves),
      %io:format("multimessage ~w~n",[MultiMessage]),
      Master ! Msg,
      leader(Id, Master, Slaves, N+1, Group, Received);
    {mcast, Msg, OldN} ->
      MultiMessage = {msg, OldN, Msg},
      bcast(Id, MultiMessage, Slaves),
      %io:format("multimessage ~w~n",[MultiMessage]),
      Master ! Msg,
      leader(Id, Master, Slaves, OldN, Group, Received);
    {join, Wrk, Peer} ->
      Slaves2 = lists:append(Slaves, [Peer]),
      Group2 = lists:append(Group, [Wrk]),
      io:format("slaves2 ~w~n",[Slaves2]),
      io:format("group2 ~w~n",[Group2]),

      bcast(Id, {view, N, [self()|Slaves2], Group2}, Slaves2), % update view
      Master ! {view, Group2},
      leader(Id, Master, Slaves2, N+1, Group2, Received);
    stop ->
      ok
    end.


slave(Id, Master, Leader, N, Last, Slaves, Group, Received) ->
  receive
    {mcast, Msg} ->
      Leader ! {mcast, Msg},
      io:format("sending multicast ~w~n",[Msg]),
      %ReceivedAdded = manageReceived(Received, Leader, N, Msg),

      slave(Id, Master, Leader, N, Last, Slaves, Group, Received);
    {join, Wrk, Peer} ->
      Leader ! {join, Wrk, Peer},
      slave(Id, Master, Leader, N, Last, Slaves, Group, Received);
    {msg, N, Msg} ->
      Master ! Msg,
      ReceivedAdded = manageReceived(Received, Leader, N, Msg, Id),
      slave(Id, Master, Leader, N+1, {msg, N, Msg}, Slaves, Group, ReceivedAdded);
    {msg, I, _} when I < N -> % discard
      slave(Id, Master, Leader, N, Last, Slaves, Group, Received);
    {view, N, [Leader | Slaves2], Group2} ->
      Master ! { view, Group2},
      slave(Id, Master, Leader, N+1, {view, [Leader|Slaves2], N, Group2}, Slaves2, Group2, Received);
      %slave(Id, Master, Leader, Slaves2, Group2);
    {'DOWN', _Ref, process, Leader, _Reason} ->
      election(Id, Master, N, Last, Slaves, Group, Received);
    stop ->
      ok
  end.

manageReceivedsdsds(Received, Leader, N, Msg, SenderId) ->
  io:format("ID ~w~n",[SenderId]),
  io:format("Message ~w~n",[Msg]),
  io:format("Received ~w~n",[Received]),
  Received.

manageReceived(Received, Leader, N, Msg, SenderId) ->
  case lists:keyfind(N,1,Received) of
    {FoundN, FoundMsg } ->
        Received;
    false ->
      UpdatedMessages = [{N,Msg} | Received],
      Leader ! {mcast, Msg, N},
      UpdatedMessages
  end.

%bcast(_Id, Msg, Nodes) ->
%    lists:foreach(fun(Node) -> Node ! Msg end, Nodes).

bcast(Id, Msg, Nodes) ->
    lists:foreach(fun(Node) -> Node ! Msg, crash(Id) end, Nodes).

crash(Id) ->
  case random:uniform(?arghhh) of
    ?arghhh ->
      io:format("leader ~w:crash~n",[Id]),
      exit(no_luck);
    _ ->
      ok
    end.


%
