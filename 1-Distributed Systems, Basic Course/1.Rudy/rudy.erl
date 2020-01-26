-module(rudy).
-export([start/1, start/2, stop/0, request/2]).

start(Port) ->
  start(Port, 1).

start(Port, N)->
  register(rudy, spawn(fun() -> init(Port,N) end)).

stop() ->
  exit(whereis(rudy), "time to die").

init(Port,N) ->
  Opt = [list, {active,false}, {reuseaddr,true}],
  case gen_tcp:listen(Port,Opt) of
    {ok, Listen} ->
      handlers(Listen,N),
      manageReceive();
    {error, Error} ->
      io:format("rudy: initialization failed: ~w~n", [Error]),
      error
    end.

manageReceive() ->
    receive
  stop ->
    ok
    end.

handlers(Client, N) ->
    case N of
  0 ->
      ok;
  N ->
      spawn(fun() -> handler(Client,N) end),
      handlers(Client, N-1)
    end.

handler(Listen, N) ->
    case gen_tcp:accept(Listen) of
  {ok,Client} ->
            request(Client, ""),
      handler(Listen, N);
  {error, Error} ->
      io:format("rudy: error in handler ~w~n", [Error]),
      error
    end.

request(Client, FirstString) ->

    Recv = gen_tcp:recv(Client,0),
    case Recv of
  {ok, Str} ->
      BodyLength = string:str(Str,"Content-length:"),
      HeadEnd = string:str(Str,"\r\n\r\n"),
      case HeadEnd of
    0 ->
        request(Client, FirstString ++ Str);
    HeadEnd ->
        Request = http:parse_request(FirstString ++ Str),
        Response = reply(Request),
        gen_tcp:send(Client, Response),
        gen_tcp:close(Client)
    end;
  {error, Error} ->
      io:format("rudy: error in request: ~w~n",[Error])
    end.

reply({{get, URI, _ }, _ , _ }) ->
  timer:sleep(40),
  http:ok(URI).
