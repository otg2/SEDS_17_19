-module(dijkstra).
-export([entry/2,replace/4,update/4, iterate/3, table/2, route/2]).

entry(Node,SortedList) ->
  FoundEntry = lists:keyfind(Node,1,SortedList),
  case FoundEntry of
    false ->
      0;
    {_, Length, _} ->
      Length
  end.

replace(Node, N, Gateway,SortedList) ->
  FoundEntry = lists:keyfind(Node,1,SortedList),
  case FoundEntry of
    %simply find a tuple of 3
     {_Node, _Length, _Gateway} ->
      UpdatedList = lists:append(lists:keydelete(Node,1,SortedList),[{Node, N, Gateway}]),
      %Returns a list containing the sorted elements of list TupleList1.
      %Sorting is performed on the Nth element of the tuples. The sort is stable.
      lists:keysort(2,UpdatedList);
    false ->
        SortedList
  end.

update(Node, N, Gateway, SortedList) ->
  CurrentLength  = entry(Node,SortedList),
  if N < CurrentLength ->
    %io:format("found ~w~n",[CurrentLength]),
    replace(Node,N,Gateway,SortedList);
  true ->
    SortedList
  end.

%test
% dijkstra:iterate(
  %[{paris, 0, paris}, {berlin, inf, unknown}],
  %[{paris, [berlin]}],
  %[]).
  %dijkstra:iterate([{paris, 0, paris}, {berlin, inf, unknown}],  [{paris, [berlin]}],[]).
% gefur[{paris, paris},{berlin,paris}]
% Instead of doing one big function with 3, create 3 different with pattern matching

%take the rst entry in the sorted list, nd the nodes in the
%map reachable from this entry and for each of these nodes update the
%Sorted list. The entry that you took from the sorted list is added to
%the routing table.

%Pattern match for empty list
iterate([],_Map,Table) ->
  Table;
%Pattern match for dummy list. Check if first node has inf
% dont care about RestOfList
iterate([{_Node, inf, _Gateway} | _RestOfInfiniteList],_Map,Table) ->
  Table;
%recursive for values needed to find
iterate([{Node, Length, Gateway} | RestOfList],Map,Table) ->
  Reaches = map:reachable(Node,Map),
  %io:format("reaches: ~w~n",[Reaches]),
  UpdatedList = lists:foldl(fun(X, Sorted) ->
    update(X, Length + 1, Gateway, Sorted ) end,
  RestOfList, Reaches ),
  %io:format("UpdatedList: ~w~n",[UpdatedList]),
  iterate(UpdatedList, Map, [{Node, Gateway}|Table]).

% dijkstra:table([paris, madrid], [{madrid,[berlin]}, {paris, [rome,madrid]}]).
% dijkstra:table([paris, madrid], []).
% [{berlin,madrid},{rome,paris},{madrid,madrid},{paris,paris}]
table(Gateways, Map) ->
  NodeCollection = map:all_nodes(Map),
  %io:format("~n~n allnotes: ~w~n",[NodeCollection]),

  GateAndNode = lists:append(Gateways,NodeCollection),
  SetOfSingles = sets:from_list(GateAndNode),
  FinalList = sets:to_list(SetOfSingles),
  %io:format("~n~n FinalList: ~w~n",[FinalList]),

  %dummy entries for all nodes with the length set to innity, inf, and the gateway to unknown.
  ListOfInfinite = lists:map(fun(Node) ->
    {Node, inf, unknown} end, FinalList),
  %io:format("infinite: ~w~n",[ListOfInfinite]),
  ListOfSelfes = lists:foldl(fun(Node, List) ->
    update(Node,0,Node, List) end, ListOfInfinite, Gateways),
  %io:format("selves: ~w~n",[ListOfSelfes]),
  %{ListOfSelfes, Map, []}.
  %ListOfSelfes = lists:keysort(2, lists:map(fun(Node) ->
							%case lists:keyfind(Node, 1, Gateways) of
							 %   false ->
								%{Node, inf, unknown};
							  %  true ->
								%{Node, 0, Node}
						%	end,
					%	end, NodeCollection)),
  iterate(ListOfSelfes,Map, []).

route(Node,Table) ->
  FoundEntry = lists:keyfind(Node,1,Table),
  case FoundEntry of
    false ->
      notfound;
    {_Node, Gateway} ->
      {ok,Gateway}
  end.
