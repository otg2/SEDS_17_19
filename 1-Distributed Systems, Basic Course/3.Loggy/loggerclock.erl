-module(loggerclock).
-export([start/1, stop/1, loop/2]).

start(Nodes) ->
  spawn_link(fun() -> init(Nodes) end).

stop(Logger) ->
  Logger ! stop.

init(Nodes) ->
  % Loggers clock
  NodesCounter = time:clock(Nodes),
  EmptyList = [], % Currently no messages in init
  loop(NodesCounter, EmptyList).


loop(NodesCounter, MessageQue) ->
  receive
    {log, From, Time, Msg } ->
      %io:format("CurrentClock: ~w~n",[NodesCounter]),
      UpdatedClock = time:update(From, Time, NodesCounter),
      %io:format("UpdatedClock: ~w~n",[UpdatedClock]),

      UpdatedMessageQue = addToQue(From,Time,Msg, MessageQue),
      %log(From, Time, Msg),
      %io:format("UpdatedClock: ~w~n",[UpdatedClock]),
      %io:format("UpdatedMessageQue: ~w~n",[UpdatedMessageQue]),

      PrintedQue = safelog(UpdatedClock, UpdatedMessageQue),
      %io:format("END OF LOOP: ~w~n",[From]),

      loop(UpdatedClock, PrintedQue);
    stop ->
      io:format("END WITH QUE LENGTH : ~w ", [length(MessageQue)]),
      io:format("END WITH QUE  : ~w ", [MessageQue]),

      ok
  end.

log(PrintFrom,PrintTime,PrintMessage) ->
  io:format("log: ~w ~w ~p~n", [PrintTime, PrintFrom, PrintMessage]).

% MessageQue
% if que is empty, start a new one
addToQue(NewFrom, NewTime, NewMessage, []) ->
  [{NewFrom,NewTime,NewMessage}];
% addToQue if it exists
% sort recursively by total order
addToQue(NewFrom, NewTime, NewMessage, MessageQue) ->
  %First entry = element(MessageQue,1)
  %io:format("CurrentQue : ~w~n",[MessageQue]),
  %[{From, Time, Msg} | MessageQue].
  %lists:keysort(2,NewList).
  % LastAddedMessage = hd(MessageQue),
  % OtherMessages = tail(messageque),
  % A better way of using hd/tail
  [LastAddedMessage | OtherMessages] = MessageQue,
  {_F, TimeOfLast, _M} = LastAddedMessage,
  case time:leq(TimeOfLast, NewTime) of
    true ->
      %io:format("true : ~w~n~n",[true]),
      [LastAddedMessage | addToQue(NewFrom, NewTime, NewFrom, OtherMessages)];
    false ->
      %io:format("false : ~w~n~n",[false]),
      [{NewFrom, NewTime, NewMessage} | MessageQue]
    end.

%% When que is empty, keep going
safelog(_InternalClock, []) -> [];
% Safe log/printout
safelog(InternalClock, MessageQue) ->
  %fails when message que is empty
  %io:format("Que as is : ~w~n",[MessageQue]),

  % oldest message should be the message with the lowest time
  % OldestMessage = hd(MessageQue),
  % RestOfMessages = tail(messageque),
  [OldestMessage | RestOfMessages] = MessageQue,
  {OldFrom, OldTime, OldMessage} = OldestMessage,
  %io:format("Time: ~w~n",[Time]),
  %io:format("InternalClock: ~w~n",[InternalClock]),

  %recursive. As soon as we hit a time that is false, no reason to keep on going
  %return the messageque
  case time:safe(OldTime, InternalClock) of
      true ->
        log(OldTime, OldFrom, OldMessage), % print it!
        safelog(InternalClock, RestOfMessages); % keep looping for more!
      false ->
        MessageQue
  end.



















%




%
