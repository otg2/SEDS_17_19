-module(test2).

-compile(export_all).

-define(Timeout, 1000).


%% Starting up a set of nodes is made easier using this function.

% cd("C:/Users/ottarg/Documents/erl/chordy").

% advanced
% f()
% test:start(node3, Main).
% test:start(node3, Main).
% test:start(node3, Main).
% Sec = test:start(node3, Main).
% test:start(node3, Main).
% Main ! probe.
% Main ! info.
% Sec ! info.
% Sec ! stop.
% @@@@@@@@@@

% Main = test:start(node3).
% test:start(node3, Main).
% test:start(node3, Main).
% test:start(node3, Main).
% Main ! probe.
% List2 = test:keys(4000).
% test:add(List2, Main).
% test:check(List2, Main).
% Sec = test2:start(node3, Main).
% test:check(List2, Sec).

% List1 = test:keys(5000).
% test:add(List1, Main).
% test:check(List1, Main).
% List2 = test:keys(10000).
% List3 = test:keys(15000).

% Main = test:start(node2).
% test:start(node2, Main).
% test:start(node2, Main).
% test:start(node2, Main).
% Main ! probe.
% List2 = test:keys(4000).
% test:add(List2, Main).
% test:check(List2, Main).
% Sec = test:start(node2, Main).
% test:check(List2, Sec).

% cd("C:/Users/ottarg/Documents/erl/chordy").

% Main = test:start(node1).
% test:start(node1, Main).
% test:start(node1, Main).
% test:start(node1, Main).
% Main ! probe.
% List2 = test:keys(4000).
% test:add(List2, Main).
% test:check(List2, Main).
% Sec = test:start(node2, Main).
% test:check(List2, Sec).

mytest() ->
  Main = test2:start(node2),
  test2:start(node2, 4, Main),
  %test2:start(node2, Main),
  %test2:start(node2, Main),
  % Main ! probe.
  List2 = test2:keys(4000),
  test2:add(List2, Main),
  test2:check(List2, Main).
  %W2 = test2:start(node2, Main),
  %test2:check(List2, W2).



% 443584618
% 723040206



start(Module) ->
    Id = key:generate(),
    apply(Module, start, [Id]).


start(Module, P) ->
    Id = key:generate(),
    apply(Module, start, [Id,P]).

start(_, 0, _) ->
    ok;
start(Module, N, P) ->
    start(Module, P),
    start(Module, N-1, P).

%% The functions add and lookup can be used to test if a DHT works.

add(Key, Value , P) ->
    Q = make_ref(),
    P ! {add, Key, Value, Q, self()},
    receive
	{Q, ok} ->
	   ok
	after ?Timeout ->
	    {error, "timeout"}
    end.

lookup(Key, Node) ->
    Q = make_ref(),
    Node ! {lookup, Key, Q, self()},
    receive
	{Q, Value} ->
	    Value
    after ?Timeout ->
	    {error, "timeout"}
    end.


%% This benchmark can be used for a DHT where we can add and lookup
%% key. In order to use it you need to implement a store.

keys(N) ->
    lists:map(fun(_) -> key:generate() end, lists:seq(1,N)).

add(Keys, P) ->
    lists:foreach(fun(K) -> add(K, gurka, P) end, Keys).

check(Keys, P) ->
    T1 = now(),
    {Failed, Timeout} = check(Keys, P, 0, 0),
    T2 = now(),
    Done = (timer:now_diff(T2, T1) div 1000),
    io:format("~w lookup operation in ~w ms ~n", [length(Keys), Done]),
    io:format("~w lookups failed, ~w caused a timeout ~n", [Failed, Timeout]).


check([], _, Failed, Timeout) ->
    {Failed, Timeout};
check([Key|Keys], P, Failed, Timeout) ->
    case lookup(Key,P) of
	{Key, _} ->
	    check(Keys, P, Failed, Timeout);
	{error, _} ->
	    check(Keys, P, Failed, Timeout+1);
	false ->
	    check(Keys, P, Failed+1, Timeout)
    end.
