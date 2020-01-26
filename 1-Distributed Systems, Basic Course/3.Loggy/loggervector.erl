-module(loggervector).
-export([start/1, stop/1, loop/3]).

start(Nodes) ->
  spawn_link(fun() -> init(Nodes) end).

stop(Logger) ->
  Logger ! stop.

init(Nodes) ->
  % Loggers clock
  %NodesCounter = vect:clock(Nodes),
  EmptyList = [], % Currently no messages in init
  InternalVector = vect:clock(Nodes),
  loop(InternalVector, EmptyList, 0).

loop(InternalVector, MessageQue, Counter) ->

    receive
      {log, From, ProcessVector, Msg } ->
        %only for debugging - testing limited amount of loops
        if Counter > -1 ->
          %io:format("NEWLINE: ~w~n", [' ']),
          %io:format("Send in : ~w ~w ~w ~n",[From, ProcessVector, InternalVector]),
          %internal order does not matter - simply add it
		        % Prepending -> Updatemessage que is New stuff + MessageQue
       % addToQue(From,ProcessVector,Msg, MessageQue),
          %UpdatedMessageQue =  [MessageQue | {From, ProcessVector, Msg}], % Why doesnt appending works?
          %UpdatedMessageQue =  [{From, ProcessVector, Msg}| MessageQue],
          %UpdatedMessageQue = lists:append(MessageQue, [{From, ProcessVector, Msg}]),
          UpdatedMessageQue =  addToQue(From,ProcessVector,Msg, MessageQue),

          UpdatedClock = vect:update(From, ProcessVector, InternalVector),
          %io:format("~n UpdatedClock: ~w~n",[UpdatedClock]),
          %io:format("InternalClock: ~w~n",[InternalVector]),
          %io:format("STARTOfSafeLog : ~n", []),

          UnPrintedQue = safelog(InternalVector, UpdatedMessageQue),

          loop(UpdatedClock, UnPrintedQue, Counter + 1);
        true ->
          true
        end;
      stop ->
        io:format("END WITH QUE LENGTH : ~w ", [length(MessageQue)]),
        io:format("END WITH QUE : ~w ", [MessageQue]),

        ok
    end.


log(Time, From, Msg) ->
  %{Time, From, Msg} = Message,
   io:format("log: ~w ~w ~w ~n", [Time, From, Msg]).

% MessageQue
addToQue(From, Time, Msg, []) ->
  [{From,Time,Msg}];

addToQue(From, Time, Msg, MessageQue) ->
  % A better way of using hd/tail
  [LastAddedMessage | OtherMessages] = MessageQue,
  {_From, TimeOfLast, _Msg} = LastAddedMessage,
  case vect:leq(TimeOfLast, Time) of
    true ->
      %io:format("true : ~w~n~n",[true]),
      [LastAddedMessage | addToQue(From, Time, Msg, OtherMessages)];
    false ->
      %io:format("false : ~w~n~n",[false]),
      [{From, Time, Msg} | MessageQue]
    end.

%% When que is empty, keep going
safelog(_InternalClock, []) ->
  %io:format("ENDOfSafeLog : ~n", []),
  [];
safelog(InternalClock, MessageQue) ->
  % for each message, loop through and return those that are unsafe
  % print those who are safe
  lists:filter(fun(Message) ->
    checkMessage(InternalClock, Message) == false end, MessageQue).

checkMessage(InternalClock,Message) ->

  % Get values from message
  {From, Time, Msg} = Message,
  %io:format("true : ~w~w~n",[Time, InternalClock]),
  case vect:safe(Time, InternalClock) of
      true ->
        %io:format("tokst :~n"),
        %io:format("INTE : ~w~n", [InternalClock]),

        log(Time, From, Msg),
        true;
        %safelog(InternalClock, RestOfMessages);
      false ->
        false
  end.


















%




%
