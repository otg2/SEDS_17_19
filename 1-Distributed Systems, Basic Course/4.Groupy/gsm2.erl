-module(gsm2).
-export([start/1, start/2]).


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
  leader(Id,Master, [],[Master]).

%slave
start(Id, Grp) ->
  Self = self(),
  {ok, spawn_link(fun() -> init(Id, Grp, Self) end)}.

init(Id, Grp, Master) ->
  Self = self(),
  Grp ! {join, Master, Self},
  receive
    {view, [Leader | Slaves], Group} ->
      erlang:monitor(process, Leader),
      Master ! {view, Group},
      %erlang:monitor(process, Leader),
      slave(Id, Master, Leader, Slaves, Group)
    after ?timeout ->
        Master ! {error, "no reply from leader"}
  end.

election(Id, Master, Slaves,  [_|Group]) ->
  Self = self(),
  case Slaves of
    [Self|Rest] ->
      bcast(Id, {view, Slaves, Group}, Rest),
      Master ! {view, Group},
      leader(Id, Master, Rest, Group);
    [Leader|Rest] ->
      erlang:monitor(process, Leader),
      slave(Id, Master, Leader, Rest, Group)
  end.

leader(Id, Master, Slaves, Group) ->
  receive
    {mcast, Msg} ->
      bcast(Id, {msg, Msg}, Slaves),
      Master ! Msg,
      leader(Id, Master, Slaves, Group);
    {join, Wrk, Peer} ->
      Slaves2 = lists:append(Slaves, [Peer]),
      Group2 = lists:append(Group, [Wrk]),
      bcast(Id, {view, [self()|Slaves2], Group2}, Slaves2),
      Master ! {view, Group2},
      leader(Id, Master, Slaves2, Group2);
    stop ->
      ok;
	Error ->
	    io:format(" ~w: leader, wtf not working ~w~n", [Id, Error])
    end.


slave(Id, Master, Leader, Slaves, Group) ->
  receive
    {mcast, Msg} ->
      Leader ! {mcast, Msg},
      slave(Id, Master, Leader, Slaves, Group);
    {join, Wrk, Peer} ->
      Leader ! {join, Wrk, Peer},
      slave(Id, Master, Leader, Slaves, Group);
    {msg, Msg} ->
      Master ! Msg,
      slave(Id, Master, Leader, Slaves, Group);
    {view, [Leader | Slaves2], Group2} ->
      Master ! { view, Group2},
      slave(Id, Master, Leader, Slaves2, Group2);
    {'DOWN', _Ref, process, Leader, _Reason} ->
      election(Id, Master, Slaves, Group);
    stop ->
      ok;
	Error ->
	    io:format(" ~w: slave, wtf not working ~w~n", [Id, Error])
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
