-module(time).
-export([zero/0, inc/2, merge/2, leq/2, clock/1, update/3, safe/2]).

zero() ->
  0.

inc(Name, T) ->
  T+1.

merge(Ti,Tj) ->
  max(Ti, Tj).

leq(Ti,Tj) ->
  if Ti > Tj -> % was >
    false;
  true ->
    true
  end.


%%% TRICKY

%forgot to use end  at the end of lists:map! remember
clock(NodesList) ->
  lists:map(fun(Node) -> {Node,0} end, NodesList).

update(Node, Time, Clock) ->
  lists:keyreplace(Node, 1, Clock, {Node, Time}). %inc(Time)

% return true or false
safe(Time, Clock) ->
  %split tuple by (names, time)
  Zipped = lists:unzip(Clock),
  %io:format("Zipped: ~w~n",[Zipped]),
  %register LowestList as list of lowest numbers
  { _Names , LowestList} = Zipped,
  %LowestList = element(Zipped,1),
  % get the first element after sorting. Should be oldest
  LowestNumber = hd(lists:sort(LowestList)),
  % if new time is higher, return que
  % else, number is lower than internal clock
  % counter so it is safe to print
  leq(Time, LowestNumber).




  %0.
