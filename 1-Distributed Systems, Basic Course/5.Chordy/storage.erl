-module(storage).
-export([create/0,add/3,lookup/2, split/3, merge/2]).

create() ->
  [].

add(Key, Value, Store) ->
  %[{Key, Value | Store}].
  lists:keystore(Key, 1, Store, {Key, Value}).

lookup(Key,Store) ->
  lists:keyfind(Key, 1, Store).


%What part should be handed over to our new predecessor? %only the lower part
% range (Pkey; Id], that is from (not including Pkey to (including) Id.
split(From, To, Store) ->
  %io:format("~n~n store ~w~n~w~n~n", [From, To]),
  %SortedStore = lists:keysort(1, Store), %.. sort to get the lower ones
  %io:format("sorted ~w~n", [SortedStore]),
  lists:partition(fun({X,_Value}) -> key:between(X, From,To) end,Store).
  %{Updated, Rest} = lists:splitwith(fun(X) -> key:between(X,From,To) end, SortedStore),
  %{Updated, Rest}.

merge(Entries, Store) ->
  lists:merge(Entries, Store).
