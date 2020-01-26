-module(interface).
-export([new/0, add/4,remove/2, lookup/2,ref/2, name/2,list/1,broadcast/2]).


% empty list
new() ->
  [].
% add new name,ref and pid to the list
add(Name, Ref, Pid, Intf) ->
  lists:append(Intf,[{Name,Ref,Pid}]).

% return a list without the found name
remove(Name, Intf) ->
  lists:keydelete(Name,1,Intf).

% next three - all the same lookup functions
% but only different parameter
% TODO: find a way to pass in a type to function. Lazy now
lookup(Name, Intf) ->
  SearchInterface = lists:keyfind(Name, 1, Intf),
  case SearchInterface of
    false ->
      notfound;
    {_Name, _Ref, Pid} ->
      {ok, Pid}
  end.
ref(Name, Intf) ->
  SearchInterface = lists:keyfind(Name, 1, Intf),
  case SearchInterface of
    false ->
      notfound;
    {_Name, Ref, _Pid} ->
      {ok, Ref}
    end.
  % This was wrong, used index of 1 instead of 2
name(Ref, Intf) ->
  SearchInterface = lists:keyfind(Ref, 2, Intf),
  case SearchInterface of
    false ->
      notfound;
    {Name, _Ref, _Pid} ->
      {ok, Name}
    end.

% get all the names in a list
list(Intf) ->
  lists:map(fun({Name,_Ref,_Pid})
    -> Name end, Intf).

% send a message to all Pid found in list
broadcast(Message, Intf) ->
  lists:foreach(fun({_Name, _Ref, Pid})
    -> Pid ! Message end, Intf).
