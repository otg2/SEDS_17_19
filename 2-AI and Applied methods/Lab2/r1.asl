// mars robot 1

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r1,X,Y).

/* Initial goal */

!check(slots). 

/* Plans */

+!check(slots) : not garbage(r1)
   <- next(slot);
      !check(slots).
+!check(slots). 


@lg[atomic]
+garbage(r1) : not .desire(carry_to(r2))
   <- !carry_to(r2).
   
+!carry_to(R)
   <- // remember where to go back
      ?pos(r1,X,Y); 
      -+pos(last,X,Y);
      
      // carry garbage to r2
      !take(garb,R);
      
      // goes back and continue to check
      !at(last); 
      !check(slots).

+!take(S,L) : true
   <- !ensure_pick(S); 
      !at(L);
      drop(S).

+!ensure_pick(S) : garbage(r1)
   <- pick(garb);
      !ensure_pick(S).
+!ensure_pick(_).

+!at(L) : at(L).
+!at(L) <- ?pos(L,X,Y);
			.print("move towards call");
           !moveOneStep(L); /* changed from original */
           !at(L).


/* moveOneStep tries to move */
/* and if it fails to move because of an obstacle it moves sideways */	  
/* The first plan is just a check to see we are at the destination and removes the */
/* guard terms q(x,moveOneStep) */ 
+!moveOneStep(l): at(L) <- .abolish(q(_,moveOneStep)).
/* Second plan, just do the normal move, add the term q(1,moveOneStep) */
/* the plan executes as long as q(1,moveOneStep) has not been put in the beliefs */
+!moveOneStep(L) : not q(1,moveOneStep) <- 
		.print("step 1");
		+q(1,moveOneStep);
		?pos(L,X,Y);
		move_towards(X,Y);
		.abolish(q(_,moveOneStep)).
/* Plan 2, change the x coordinate to move around the obstacle */
/* uses guard q(2,moveOneStep) */
/* a better version of this would look at the current r1 position and use it as the */
/* the coordinate to modify for the the call to move_towards */
+!moveOneStep(L) : not q(2,moveOneStep) <-
		.print("step 2 ");
		+q(2,moveOneStep);
		?pos(L,X,Y);
		move_towards(X+1,Y);
		.abolish(q(_,moveOneStep)).
/* this catches the failure of moveOneStep (note the -! syntax) */
/* it asserts the gaol again to retry to achieve the move */
-!moveOneStep(L) : true <- 
		.print("do fail");
		!moveOneStep(L).

		
		
	
