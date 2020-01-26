-module(history).
-export([new/1,update/3]).

%set new history name with 0 counter since we are
% using the latter method described in 4 The history
new(Name) ->
  [{Name, inf}]. %0

% update function
update(Node, N, History) ->
  % find the node from history. Node is #1 in tuple
  FoundNode = lists:keyfind(Node,1,History),
  case FoundNode of
    %if found, get the node and the number
    {Node, LastNumber} ->
      % is we have a higher number then the one registred
      if N > LastNumber ->
        % return new, list where the old one is removed and the new one added to the list
        {new, lists:append(lists:keydelete(Node,1,History),[{Node, N}]) };
      true ->
        %else the message is old
      old
    end;
  % if it wasn't found, create new one with the same history
    false ->
      {new,[{Node,N} | History]}
  end.
