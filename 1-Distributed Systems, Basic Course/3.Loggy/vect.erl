-module(vect).
-export([zero/0, inc/2, merge/2, leq/2, clock/1, update/3, safe/2]).

zero() ->
  0.

inc(Name, Vector) ->
  case lists:keyfind(Name,1,Vector) of
    {Node, Count } ->
        lists:keyreplace(Name,1,Vector, {Node, Count + 1});
    false ->
      % Should never happen since I initialize it in the first place
      [{Name, 1} | Vector]
  end.

% VectorOne = [{john,20},{paul,1},{ringo,9},{george,6}].
% VectorTwo = [{john,1},{paul,0},{ringo,5},{george,7}].

merge([], Vector) -> Vector;

merge([{Name, Ti} | Rest], Vector) ->

  %io:format("lookfor: ~w~n ", [{Name, Ti}]),
  %io:format("Rest: ~w~n ", [Rest]),
  %io:format("Vector: ~w~n ", [Vector]),
  case lists:keyfind(Name, 1, Vector) of
    {Found, Tj }->
        %io:format("in: ~w ~w~n ", [Rest, lists:keydelete(Found, 1, Vector)]),
        [{Found, max(Ti,Tj)} | merge(Rest, lists:keydelete(Found, 1, Vector))];
    false ->
      %io:format("false: ~w~n ", [false]),
      [{Name, Ti} | merge(Rest, Vector)]
  end.

leq([],_) ->
  true;

leq([{Name, Ti} | Rest], Vector) ->
  case lists:keyfind(Name, 1, Vector) of
    {Name, Tj} ->
      if Ti =< Tj ->
        leq(Rest,Vector);
      true ->
        false
    end;
  false ->
    false
  end.


clock(NodesList) ->
  lists:map(fun(Node) -> {Node,vect:zero()} end, NodesList).

update(Node, Vector, Clock) ->
  {FoundNode, FoundCount} = lists:keyfind(Node, 1, Clock),
  case lists:keyfind(FoundNode, 1, Vector) of
    {From, Count} ->
      lists:keyreplace(From, 1, Clock, {From, Count});
    false ->
      [{Node, FoundCount} | Clock]
  end.
% [{john,1},{paul,0},{ringo,1},{george,0}]
% [{john,0},{paul,0},{ringo,1},{george,0}]

%TIME : [{john,0},{paul,1},{ringo,0},{george,0}]
%INTE : [{john,1},{paul,0},{ringo,2},{george,0}]

safe(Time, Clock) ->
  case leq(Time, Clock) of
    true ->
      true;
    false ->
      false
    end.
  %[NodeCheck | Rest] = Time,
  %{NameOfCheck, TimeOfCheck} = NodeCheck,
  %% {john, 0}
  %%{FoundNode, FoundCount} = lists:keyfind(NameOfCheck, 1, Clock),
  %case lists:keyfind(NameOfCheck, 1, Clock) of
    %{FoundFrom, FoundTime} ->
      %%{john, 1}
      %if TimeOfCheck > FoundTime ->
        %%io:format("SAFE FOUND : ~n", []),
        %%io:format("NodeCheck : ~w~n", [NodeCheck]),
        %%io:format("InteCheck : ~w~n", [{FoundFrom, FoundTime}]),

        %true;
    %true ->
     %false
  %end;
  %false ->
    %false
%end.
