-module(gsm6).
-export([start/1, start/2, sendLostMessage/3]).


%-define(CODE, "Z00887").
-define(timeout, 2000).
-define(arghhh, 100).

%master
start(Id) ->
  Rnd = random:uniform(1000),
  Self = self(),
  {ok, spawn_link(fun() -> initleader(Id, Rnd, Self) end)}.

initleader(Id, Rnd, Master) ->
  random:seed(Rnd, Rnd, Rnd),
  History = [],
  leader(Id,Master, [], 0, [Master], History).

%slave
start(Id, Grp) ->
  Rnd = random:uniform(1000),
  Self = self(),
  {ok, spawn_link(fun() -> initslave(Id, Grp, Rnd, Self) end)}.

initslave(Id, Grp, Rnd, Master) ->
  random:seed(Rnd, Rnd, Rnd),
  Self = self(),
  Grp ! {join, Master, Self},
  receive
    {view, N, [Leader | Slaves], Group} ->
      %erlang:monitor(process, Leader),
      LastMessage = {view, N, [Leader | Slaves], Group},
      io:format("leaderinit ~w~n",[LastMessage]),
      Master ! {view, Group},
      erlang:monitor(process, Leader),
      slave(Id, Master, Leader, N+1, LastMessage, Slaves, Group) % send last view
    after ?timeout ->
        Master ! {error, "no reply from leader"}
  end.

election(Id, Master, N, Last, Slaves, [_|Group]) ->
  Self = self(),
  case Slaves of
    [Self|Rest] ->
      bcast(Id, Last, Rest), % the new leaders last message, includes the n
      %io:format("self ~w~n",[Last]),
      NewMessage = {view, N, Slaves, Group}, % the new view
      %io:format("self ~w~n",[NewMessage]),
      bcast(Id, NewMessage , Rest),
      Master ! {view, Group},
      NewHistory = [NewMessage, Last], % How to get the old history?
      leader(Id, Master, Rest, N+1, Group, NewHistory);
    [Leader|Rest] ->
      erlang:monitor(process, Leader),
      io:format("leader ~w~n",[Last]),
      slave(Id, Master, Leader, N, Last, Rest, Group)
  end.

manageHistory(Message, List) ->
  UpdatedList = [Message | List],
  if length(UpdatedList) > 10 ->
      lists:droplast(UpdatedList);
    true ->
      UpdatedList
  end.

leader(Id, Master, Slaves, N, Group, History) ->
  receive
    {mcast, Msg} ->
      MultiMessage = {msg, N, Msg},
      bcast(Id, MultiMessage, Slaves),
      UpdatedHistory = manageHistory(MultiMessage, History),

      %io:format("updated  ~w~n",[length(UpdatedHistory)]),
      Master ! Msg,
      leader(Id, Master, Slaves, N+1, Group, UpdatedHistory);
    {join, Wrk, Peer} ->
      Slaves2 = lists:append(Slaves, [Peer]),
      Group2 = lists:append(Group, [Wrk]),
      io:format("slaves2 ~w~n",[Slaves2]),
      io:format("group2 ~w~n",[Group2]),

      bcast(Id, {view, N, [self()|Slaves2], Group2}, Slaves2), % update view
      ViewMessage = {view, Group2},
      Master ! ViewMessage,
      UpdatedHistory = [ViewMessage | History],
      %io:format("updated  ~w~n",[UpdatedHistory]),
      leader(Id, Master, Slaves2, N+1, Group2, UpdatedHistory);
    {lostmessage, N, SenderId} ->
      sendLostMessage(N,History, SenderId),
      leader(Id, Master, Slaves, N, Group, History);
    stop ->
      ok
    end.

%Note : these might be marked with some kind of safe message even though the timestamp is lower
sendLostMessage(0,_List, _Sender) ->
   ok;
sendLostMessage(N,List, SenderId) when N > 0 ->
  [Young | Old] = List,
  [SenderId ! {safemessage, Young} | sendLostMessage(N-1,Old, SenderId)].


slave(Id, Master, Leader, N, Last, Slaves, Group) ->
  receive
    {mcast, Msg} ->
      Leader ! {mcast, Msg},
      %io:format("not ~w~n",[Msg]),

      slave(Id, Master, Leader, N, Last, Slaves, Group);
    {join, Wrk, Peer} ->
      Leader ! {join, Wrk, Peer},
      slave(Id, Master, Leader, N, Last, Slaves, Group);
    {msg, N, Msg} ->
      Master ! Msg,
      slave(Id, Master, Leader, N+1, {msg, N, Msg}, Slaves, Group);
    {msg, I, _} when I < N ->
      slave(Id, Master, Leader, N, Last, Slaves, Group);
    % WHEN WE HAVE LOST A MESSAGE
    % Should be marked with a safe message even if timestamp is lower
    {msg, I, _} when I > N ->
      io:format("happens ~w > ~w~n",[I, N]),
      Leader ! {lostmessage, I-N, self()},
      slave(Id, Master, Leader, N, Last, Slaves, Group);
    {safemessage,Msg } ->
      Master ! Msg,
      slave(Id, Master, Leader, N, Last, Slaves, Group);
    {view, N, [Leader | Slaves2], Group2} ->
      Master ! { view, Group2},
      slave(Id, Master, Leader, N+1, {view, [Leader|Slaves2], N, Group2}, Slaves2, Group2);
      %slave(Id, Master, Leader, Slaves2, Group2);
    {'DOWN', _Ref, process, Leader, _Reason} ->
      election(Id, Master, N, Last, Slaves, Group);
    stop ->
      ok
  end.

%bcast(_Id, Msg, Nodes) ->
%    lists:foreach(fun(Node) -> Node ! Msg end, Nodes).

bcast(Id, Msg, Nodes) ->
    io:format("nodes ~w~n",[Nodes]),
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
