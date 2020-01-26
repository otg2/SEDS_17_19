-module(workervector).
-export([start/6,stop/1, peers/2]).


start(Name, Logger, Seed, Sleep, Jitter, Nodes) ->
  % create vector with number of Nodes
  Vector = lists:map(fun(Node) -> {Node,vect:zero()} end, Nodes),
  %[{john, 3}, {ringo, 2}, {paul, 4}, {george, 1}]
  spawn_link(fun()-> init(Name, Logger, Seed, Sleep, Jitter, Vector) end).


stop(Worker) ->
  Worker ! stop.


init(Name, Log, Seed, Sleep, Jitter, Vector) ->
  random:seed(Seed, Seed, Seed),
  receive
    {peers, Peers} ->
      loop(Name, Log, Peers, Sleep, Jitter, Vector);
    stop ->
      ok
    end.

peers(Wrk, Peers) ->
  Wrk ! {peers, Peers}.

loop(Name, Log, Peers, Sleep, Jitter, Vector) ->
  Wait = random:uniform(Sleep),
  receive
    {msg, OtherVector, Msg} ->
      %io:format("Vector : ~w~n ", [Vector]),

      %io:format("IncrVector: ~w~n ", [IncrVector]),
      %io:format("OtherVector: ~w~n ", [OtherVector]),
      MergedVector = vect:merge(OtherVector,Vector),
      IncrVector = vect:inc(Name, MergedVector),

      %io:format("MergedVector : ~w~n ", [MergedVector]),
      Log ! {log, Name, IncrVector, {received, Msg}},
      loop(Name,Log,Peers,Sleep,Jitter,IncrVector);
    stop ->
      ok;
    Error ->
      log ! {log, Name, time, {error, Error}}
  after Wait ->
    Selected = select(Peers),
    IncrVector = vect:inc(Name, Vector),
    Message = {hello, random:uniform(100)},
    Selected ! {msg, IncrVector, Message},
    jitter(Jitter),
    Log ! {log, Name, IncrVector, {sending, Message}},
    loop(Name, Log, Peers, Sleep, Jitter, IncrVector)
  end.

select(Peers) ->
  lists:nth(random:uniform(length(Peers)), Peers).

jitter(0) ->
  ok;
jitter(Jitter) ->
  timer:sleep(random:uniform(Jitter)).
