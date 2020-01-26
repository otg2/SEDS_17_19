-module(test).
-export([run/2, runlamb/2, runvector/2]).


run(Sleep, Jitter) ->
  io:format("STARTING RUN WITH SLEEP: ~w AND JITTER ~w ~n~n~n~n",[Sleep, Jitter]),
  Log = logger:start([john, paul, ringo, george]),
  A = worker:start(john, Log, 13, Sleep, Jitter),
  B = worker:start(paul, Log, 23, Sleep, Jitter),
  C = worker:start(ringo, Log, 33, Sleep, Jitter),
  D = worker:start(george, Log, 43, Sleep, Jitter),
  worker:peers(A, [B,C,D]),
  worker:peers(B, [A,C,D]),
  worker:peers(C, [A,B,D]),
  worker:peers(D, [A,B,C]),
  timer:sleep(5000),
  logger:stop(Log),
  worker:stop(A),
  worker:stop(B),
  worker:stop(C),
  worker:stop(D).

runlamb(Sleep, Jitter) ->
  io:format("STARTING RUN_LAMB WITH SLEEP: ~w AND JITTER ~w ~n~n~n~n",[Sleep, Jitter]),

  Log = loggerclock:start([john, paul, ringo, george]),
  A = workerlamb:start(john, Log, 13, Sleep, Jitter),
  B = workerlamb:start(paul, Log, 23, Sleep, Jitter),
  C = workerlamb:start(ringo, Log, 33, Sleep, Jitter),
  D = workerlamb:start(george, Log, 43, Sleep, Jitter),
  workerlamb:peers(A, [B,C,D]),
  workerlamb:peers(B, [A,C,D]),
  workerlamb:peers(C, [A,B,D]),
  workerlamb:peers(D, [A,B,C]),
  timer:sleep(5000),
  loggerclock:stop(Log),
  workerlamb:stop(A),
  workerlamb:stop(B),
  workerlamb:stop(C),
  workerlamb:stop(D).


runvector(Sleep, Jitter) ->
  io:format("STARTING RUN_VECTOR WITH SLEEP: ~w AND JITTER ~w ~n~n~n~n",[Sleep, Jitter]),
  Nodes = [john, paul, ringo, george],
  Log = loggervector:start(Nodes),
  A = workervector:start(john, Log, 13, Sleep, Jitter, Nodes),
  B = workervector:start(paul, Log, 23, Sleep, Jitter, Nodes),
  C = workervector:start(ringo, Log, 33, Sleep, Jitter, Nodes),
  D = workervector:start(george, Log, 43, Sleep, Jitter, Nodes),
  workervector:peers(A, [B,C,D]),
  workervector:peers(B, [A,C,D]),
  workervector:peers(C, [A,B,D]),
  workervector:peers(D, [A,B,C]),
  timer:sleep(5000),
  loggervector:stop(Log),
  workervector:stop(A),
  workervector:stop(B),
  workervector:stop(C),
  workervector:stop(D).
