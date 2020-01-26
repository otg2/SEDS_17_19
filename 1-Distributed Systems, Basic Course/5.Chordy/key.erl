-module(key).
-export([generate/0, between/3]).


%The between/3 function will check if a Key is between From and To or
%equal to To, this is called a partly closed interval and is denoted (From; To].
%Remember that the we're dealing with a ring so it could be that From
%is larger than To. What does that mean and how do you handle it? Also,
%From could be equal to To and we will interpret this as the full circle i.e.
%anything is in between.

generate() ->
  random:uniform(1000000000).

between(Key, From, To) ->
    if
       From == To ->
        true;
	     (From < To)
       and
       (Key > From)
       and
       (Key =< To) ->
	        true;
	     (From > To)
          and
        (
          (Key > From)
          or
          (Key =< To)
        ) ->
	        true;
       true ->
	        false
    end.

betweenOld(Key, From, To) ->

  if From == To ->
    true;
  true ->
    %Max = max(From,To),
    %Min = min(From,To),

    if From < Key ->
      if Key =< To ->
        true;
      true ->
        false
      end;
    true ->
      false
    end
  end.
