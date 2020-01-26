-module(routy).
-export([start/2, stop/1, router/6]).

start(Reg,Name) ->
  register(Reg, spawn(fun() -> init(Name) end)).

% werl -sname iceland -setcookie routy -connect_all false
% werl -sname sweden -setcookie routy -connect_all false

stop(Node) ->
  Node ! stop,
  unregister(Node).

init(Name) ->
  Intf = interface:new(),
  Map = map:new(),
  Table = dijkstra:table(Intf, Map),
  Hist = history:new(Name),
  router(Name,0, Hist,Intf,Table,Map).


router(Name, N, Hist, Intf, Table, Map) ->
  receive
    %add. rvk, {r1,'iceland@FI16266'}
    {add, Node, Pid} ->
      Ref = erlang:monitor(process, Pid),
      io:format("ref ~w~n",[Ref]),
      io:format("Intf ~w~n",[Intf]),
      Intf1 = interface:add(Node,Ref,Pid,Intf),
      io:format("Intf1 ~w~n",[Intf1]),
      router(Name,N,Hist,Intf1,Table,Map);
    %remove. rvk
    {remove, Node} ->
      {ok, Ref} = interface:ref(Node, Intf),
      erlang:demonitor(Ref),
      Intf1 = interface:remove(Node, Intf),
      router(Name,N,Hist,Intf1,Table,Map);
    %links. rvk, ???, [london, paris, berlin]
    {links, Node, R, Links} ->
      case history:update(Node,R,Hist) of
        {new, Hist1} ->
          interface:broadcast({links,Node,R,Links}, Intf),
          Map1 = map:update(Node,Links,Map),
          router(Name,N,Hist1, Intf, Table, Map1);
        old ->
          router(Name, N, Hist, Intf, Table, Map)
        end;
    %update
    update ->
      io:format("Intf ~w~n",[Intf]),
      ListOfInterface = interface:list(Intf),
      io:format("List ~w~n",[ListOfInterface]),
      %dijkstra:table([paris, madrid], [{madrid,[berlin]}, {paris, [rome,madrid]}]).
      %dijkstra:table([paris, madrid], []).
      Table1 = dijkstra:table(ListOfInterface, Map),
      io:format("Table ~w~n",[Table1]),
	    router(Name, N, Hist, Intf, Table1, Map);
    %broadcast
    broadcast ->
	    Message = {links, Name, N, interface:list(Intf)},
      io:format("broadcast ~w~n",[Message]),
	    interface:broadcast(Message, Intf),
	    router(Name, N+1, Hist, Intf, Table, Map);
    %{'DOWN', rvk, ...}
    {'DOWN', Ref, process, _, _} ->
      {ok, Down} = interface:name(Ref,Intf),
      io:format("~w: exit received from ~w~n",[Name,Down]),
      Intf1 = interface:remove(Down, Intf),
      router(Name,N,Hist,Intf1,Table,Map);
    %c/p
    {route, Name, From, Message} ->
	    io:format("~w: received message ~w from ~w~n", [Name, Message,From]),
	    router(Name, N, Hist, Intf, Table, Map);
  	{route, To, From, Message} ->
  	    io:format("~w: routing message (~w)", [Name, Message]),
  	    case dijkstra:route(To, Table) of
  		{ok, Gw} ->
  		    case interface:lookup(Gw, Intf) of
  			{ok, Pid} ->
  			    Pid ! {route, To, From, Message};
  			notfound ->
  			    ok
  		    end;
  		notfound ->
  		    ok
  	    end,
  	    router(Name, N, Hist, Intf, Table, Map);
  	{send, To, Message} ->
	    self() ! {route, To, Name, Message},
	    router(Name, N, Hist, Intf, Table, Map);
    {status, From} ->
      From ! {status, {Name,N,Hist,Intf,Table, Map}},
      router(Name,N,Hist,Intf, Table, Map);
    % end of c/p
    stop ->
      ok;
    status ->
      io:format("Name: ~w\n N: ~w\nHistory: ~w\nInterfaces: ~w\nTable: ~w\nMap: ~w\n", [Name, N, Hist, Intf, Table, Map]),
      router(Name,N,Hist,Intf, Table, Map)
  end.
