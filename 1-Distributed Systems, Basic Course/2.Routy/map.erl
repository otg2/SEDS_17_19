-module(map).
-export([new/0, update/3, reachable/2,all_nodes/1]).

%MapTest =[{berlin,[london,paris]},{london,[berlin,paris]}].
%MapAdd =[{berlin,[london,paris]},{london,[berlin,paris]},{paris,[berlin,london]}].
%map:update(berlin, [madrid], [{berlin,[london,paris]}]).

new() ->
  [].

update(Node, Links, Map) ->
  lists:append(lists:keydelete(Node,1,Map),[{Node, Links}]).

reachable(Node,Map) ->
  MatchNode = lists:keyfind(Node,1,Map),
  case MatchNode of
    false ->
      [];
    {Node, Reaches} ->
      Reaches
  end.

  %0.

%https://stackoverflow.com/questions/13673161/remove-duplicate-elements-from-a-list-in-erlang

all_nodes(Map) ->
  EmptyList = [],
  List = lists:foldl(fun({Node,Links}, Final) ->
    [Node|Links] ++ Final end, EmptyList, Map),
  SetOfSingles = sets:from_list(List),
  sets:to_list(SetOfSingles).
