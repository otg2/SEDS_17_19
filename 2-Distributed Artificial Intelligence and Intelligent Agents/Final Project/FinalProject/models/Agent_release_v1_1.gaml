/**
 *  Agent
 *  Author: ottarg and xin 
 *  Description: sweet stuff
 */

model Agent_release_v1_1

global {
	file building_shapefile <- file("../includes/building.shp");
	geometry shape <- envelope(building_shapefile);
	int max_memory <- 5;
	
	int mapSize_X <- 200;
	int mapSize_Y <- 200;
	int controllerRange <- 50;
	int deliveryRange <- 1;
	
	
	int peopleSpeed_rand <- 2;
	int max_peopleSpeed <- 3;
	float people_size <- 2.0;
	float supplies_size <- 4.0;
	float camp_size <- 4.0;
	float control_size <- 5.0;
	
	int nb_deliveryman<-9;
	int nb_supplies <- 3;
	int nb_camp <-10;
	int nb_requester <- nb_camp;
	int nb_control<-1;
	
	point target_point_avg; 
	point target_point_weigh;
	point target_point_traverse;
	point target_point_norm;
	
    list<camp> availableCamps;
	list<supplies> availableSupplies;
	
	int scenario_type;
	float restrictionFactor;
	
	//int daylight_hour update: 70 + 30 * sin(cycle/10);
	
	init 
	{
		// STATIC OBJECTS
		// Create the buildings
		create obsticle from: building_shapefile
		{
			ask cell overlapping self {
				is_obstacle <- true;
				color <- rgb("black");
			}
		}
		
		// Get all available cells
		list<cell> free_cell <- cell where not (each.is_obstacle);//and each.is_obstacle) ;
		
		// SCENARIO SETUP - hardcoded for now, we only have 5 scenarios
		// 1 is free for all 
		// supplies | camps
		list<cell> _restrictCells <- [];
		if(scenario_type = 2)
		{
			_restrictCells <- (free_cell where (each.location.x < mapSize_X*restrictionFactor));
		}
		
		// camps | supplies
		else if(scenario_type = 3)
		{
			_restrictCells <- (free_cell where (each.location.x > mapSize_X*restrictionFactor));
		}
		// camps | supplies | camps
		else if(scenario_type = 4)
		{
			// TODO: Make circle/range instead?
			float _restrictSplit <- restrictionFactor/2;
			_restrictCells <- (free_cell where (each.location.x > mapSize_X*_restrictSplit and each.location.x < mapSize_X*(1-_restrictSplit)));
		}
		// supplies | camps | supplies
		else if(scenario_type = 5)
		{
			// TODO: Make circle/range instead?
			float _restrictSplit <- restrictionFactor/2;
			_restrictCells <- (free_cell where (each.location.x < mapSize_X*_restrictSplit or each.location.x > mapSize_X*(1-_restrictSplit)));
		}
		// Set all cells found as "is_suppl
		ask _restrictCells 
		{
			is_supply <- true;
			//color <- rgb(229,136,236);
		}
		//_restrictCells <- nil;
		free_cell <- free_cell + _restrictCells;
		
		// Clear memory
		_restrictCells <- nil;
		//free_cell <- cell where not (each.is_obstacle or not each.is_supply); 
		// Create supplies
		create control number: nb_control
		{
			current_cell <-one_of(free_cell);
			location <- current_cell.location;
			current_cell.is_free <- false;
			size <- control_size;
			
			ask cell overlapping self {
				color <- rgb(226,126,126);
			}
		}
		
		create supplies number: nb_supplies 
		{
			current_cell <-one_of(free_cell);
			location <- current_cell.location;
			current_cell.is_free <- false;
			remove current_cell from: free_cell;
			
			// Remove all cells in range of supply depoit
			// Make sure no camp is in range of supply
			size <- supplies_size;
			current_cell.is_supply <- true;
			ask cell at_distance controllerRange{//overlapping self {
				is_supply <- true;
				//color <- rgb(229,136,236);
			}
			ask control{
				allsupplies<-allsupplies+myself;
			}
		}
		free_cell <- cell where not (each.is_obstacle or each.is_supply); 
	
		
		// Create the camps
		create camp number: nb_camp
		{
			current_cell <-one_of(free_cell);
			location <- current_cell.location;
			current_cell.is_free <- false;
			size <- camp_size;
			
			ask cell overlapping self {
				color <- rgb(226,126,126);
			}
		}
		
		availableCamps <- camp;
		availableSupplies <- supplies;		
		
		create deliveryman number: nb_deliveryman 
		{
			startingSupplies <-one_of(availableSupplies);
			headColor <- startingSupplies.color;
			current_cell <- startingSupplies.current_cell;
			current_cell.is_free <- false;
			remove current_cell from: free_cell;
			location <- current_cell.location;
			speed <- float(rnd(peopleSpeed_rand) + 1);
			memory << current_cell;
			loop aSupplies over:supplies{
				ask aSupplies{
				deliverymen<-deliverymen+myself;
			    } 
			}
		}
		
		create requester number: nb_requester 
		{
			startingCamp <-one_of(availableCamps);
			remove startingCamp from:availableCamps;
			//startingCamp <-myCamp;
			headColor <- startingCamp.color;
			current_cell <- startingCamp.current_cell;
			current_cell.is_free <- false;
			remove current_cell from: free_cell;
			location <- current_cell.location;
			speed <- float(rnd(peopleSpeed_rand) + 1);
			memory << current_cell;
		}
	}
}



species obsticle 
{
	float height <- 3.0 + rnd(5);
	aspect default 
	{
		draw shape color: rgb("gray") depth: height;
	}
}

species people skills:[moving, communicating] {
	float size <- people_size;
	cell current_cell;
	cell home_cell;
	list<cell> memory;
	float speed;
	cell target_cell;
	rgb headColor;
	
	init
	{
		// Set the home cell as the starting cell
		home_cell <- current_cell;
		target_cell<-home_cell;
	}
	reflex move_1 when:((current_cell.location distance_to target_cell.location)>speed)
	{
		list<cell> possible_cells <- current_cell neighbours_at speed where (not (each.is_obstacle) and each.is_free and not (each in memory));
		// Keep moving if not there
		if not empty(possible_cells) {
			current_cell.is_free <- true;
			current_cell <- shuffle(possible_cells) with_min_of (each.location distance_to target_cell.location);
			location <- current_cell.location;
			current_cell.is_free <- false;
			memory << current_cell; 
			if (length(memory) > max_memory) {
				remove memory[0] from: memory;
			}
		}
	}
	reflex move_2 when:((current_cell.location distance_to target_cell.location)<=speed and current_cell.location!=target_cell.location)
	{
		current_cell.is_free <- true;
		current_cell <- target_cell;
		location <- current_cell.location;
		current_cell.is_free <- false;
		memory << current_cell; 
		if (length(memory) > max_memory) {
			remove memory[0] from: memory;
		}
	}
}

species requester parent:people {
	rgb color <- rgb(rnd(255),rnd(255),rnd(255));
	int start_cycle;
	camp startingCamp;
	// The amount they are currently hold
	//float supplyAmount;
	// Just to make sure we only send a single request. Should implement a FSM for this? State=Monitoring -> State=Requesting...
	//bool monitorMode;		
	init
	{
		start_cycle <-rnd(10);
		write 'requester '+string(self) +' started binding to camp '+ string(startingCamp);
		//monitorMode <- true;
		//supplyAmount <- 0.0;
	}
	
	/*reflex callForSupplies when: monitorMode 
	{
		if(startingCamp.storage < startingCamp.threshHold_level)
		{
			monitorMode <- false;
			do send with: [receivers:: supplies, content:: ['I need supplies.'] ,performative::'request' ,protocol:: 'request_supplies'];
			write 'receiver '+string(id) +' sent a message';
		}
		
	}
	
	reflex suppliesToCamp when: !monitorMode and supplyAmount > 0.0
	{
		startingCamp.storage <- supplyAmount;
		supplyAmount <- 0;
		monitorMode <- true;
	}*/
	reflex request when: cycle=start_cycle
	{
		do send with: [receivers:: control, content:: ['I need supplies.'] ,performative::'request' ,protocol:: 'request_supplies'];
		write 'requester '+string(self) +' sent a message to control center';
	}
	reflex handle_reply_from_supplies_1 when: (!empty(informs)) 
	{
		write 'requester '+string(self) +' received a reply from supply station to meet a deliveryman';
		message replyfromsupplies <- informs at 0;
	    deliveryman best_dm <- replyfromsupplies.content at 0;
	    color <-best_dm.color;
	    do send with: [receivers:: [best_dm], content:: ['I need supplies.'] ,performative::'request' ,protocol:: 'request_supplies'];
	    remove index:0 from: informs;
	}
	
	reflex handle_reply_from_supplies_2 when: (!empty(refuses)) 
	{
		write 'requester '+string(self) +' received a reply from supply station to pick up at station';
		message replyfromsupplies <- refuses at 0;
	    supplies closest_supplies <- replyfromsupplies.content at 0;
	    color <-closest_supplies.color;
	    target_cell <- closest_supplies.current_cell;
	    remove index:0 from: refuses;
	}
	
	reflex handle_reply_from_deliveryman when: (!empty(proposes)) 
	{
		write 'requester '+string(self) +' received a reply from deliveryman';
		message replyfromdeliveryman <- proposes at 0;
	    target_cell <- replyfromdeliveryman.content at 0;
	    remove index:0 from: proposes;
	}
	aspect default {
		if(current_cell != home_cell){
		  draw pyramid(size) at: {location.x,location.y,3}  color: headColor;
		  draw sphere(size) at: {location.x,location.y,size} color: color;
		}
	}
}

species deliveryman parent:people {
	rgb color <- rgb(rnd(255),rnd(255),rnd(255));
	supplies startingSupplies;
	list<requester> requesters;
	meetingpoint mymeetingpoint;
	//float carryAmount;
	
	init
	{
		write 'deliveryman '+string(self) +' started binding to supply station '+ string(startingSupplies);
		create meetingpoint number: 1 returns: themeetingpoint
		{
			color <- myself.color;
		}
		//carryAmount<- 300.0;
		mymeetingpoint <-themeetingpoint at 0;
	}
	
	reflex deliver when: !empty(requesters at_distance deliveryRange) // deliveryRange
	{
		ask requesters at_distance deliveryRange
		{
			
			//TODO: Give him the exact amount that he needs if we have enough and have priority?
			// like myself.carryAmount <- myself.carryAmount - self.neededAmount;  
			// and 
			// self.supplyAmount <- self.neededAmount
			// Give him supplies and tell him to go home :))))
			//myself.carryAmount <- myself.carryAmount - 100.0;
			//self.supplyAmount <- 100.0;
			self.target_cell <- self.home_cell;
			
			// get receiver and remove from list
			remove self from: myself.requesters;
		}
	}
	
	reflex travel_back_to_supplies when: empty(requesters) // deliveryRange
	{
		
		supplies newHome <- supplies closest_to self;
		target_cell <-newHome.current_cell;
		mymeetingpoint.mylocation <-nil;
		// Refill supplies
		/*if((self.location distance_to home_cell.location) < 2)
		{
			carryAmount <- 300.0;
		}*/
	}
	
	
	// NOTE: only handle requests when we can deliver?
	reflex handle_request when: (!empty(messages)) //and carryAmount > 0
	{ 
		write 'deliveryman '+string(self)+' received a message';
	    message requestfromrequester <- messages at 0;
	    requesters <- requesters+requestfromrequester.sender;
	    
	    list<people> allLocations <- requesters+self;
		int n <- length(allLocations);
		int max_speed <- 0;
    	int min_speed <- 1000;
		int total_speed <- 0;
		loop p over:allLocations
		{
			max_speed <- int(max([max_speed, p.speed]));
			min_speed <- int(min([min_speed, p.speed]));
			total_speed <- total_speed + p.speed;
		}
		
		float total_factors <- 0;
		loop p over:allLocations
		{
			total_factors <- float(total_factors+ float(total_speed / p.speed));
		}
		
		float avg_x_weigh <- 0.0;
		float avg_y_weigh <- 0.0;
		
		loop p over:allLocations
		{
			avg_x_weigh <- avg_x_weigh + (p.location.x)* weight(p.speed, total_speed, total_factors);
			avg_y_weigh <- avg_y_weigh + (p.location.y)* weight(p.speed, total_speed, total_factors);
		}
		
		// weigh # green
		target_point_weigh <- {avg_x_weigh,avg_y_weigh,0};
		
		// Set the agent target
		target_cell <- cell closest_to target_point_weigh;
		mymeetingpoint.mylocation <-target_point_weigh;
		do send with: [receivers:: requesters, content:: [target_cell] ,performative::'propose' ,protocol:: 'request_supplies'];
	    if (!empty(messages)){
	    	remove index:0 from: messages;
	    }
	}
	
	aspect default {
		if(current_cell != home_cell){
		  draw pyramid(size) at: {location.x,location.y,3}  color: headColor;
		  draw sphere(size) at: {location.x,location.y,size} color: color;
		}
	}
	
	float weight(float x, int totalSpeed, float totalFactor)
	{
		return  (totalSpeed/x) / totalFactor;
	}
}

species building
{
	cell current_cell;
	list<cell> memory;
	float size;
	
}

species camp parent:building
{
	rgb color <- rgb("red");
	//float storage;
	//float consume_rate;
	//float threshHold_level;
	
	// We can create with random variables. Just set it here for demonstration
	/*init
	{
		storage <- float(50 + rnd(30));
		consume_rate <- 2.0 + rnd(1.9);
		threshHold_level <- 20.0 + rnd(10);
	}
	// TODO: Change the consume rate when camp is at critical level?
	// Only consume supplies at camps when the storage is bigger than the consumption of supplies
	reflex usage when:  storage > consume_rate
	{
		storage <- storage - consume_rate;
	}*/
	init
	{
		write 'camp '+string(self) +' started';
	}
	aspect default {
		draw pyramid(size) at: {location.x,location.y,0} color: color;
	}
	
}

species supplies parent:building skills:[communicating]
{
	list<deliveryman> deliverymen;
	rgb color <- rgb("blue");
	
	string supplyType;
	init
	{
		write 'supply station '+string(self) +' started';
	}
	reflex handle_request when: (!empty(messages)) {
		write 'supply station '+string(self)+' received a message from control center';
	    message requestfromcontrol <- messages at 0;
	    requester from<-requestfromcontrol.content at 0;
	    deliveryman best_dm <- deliverymen with_min_of (each.target_cell distance_to from);
	    if ((best_dm.location distance_to from.location)>(from.location distance_to location)){
	    	do send with: [receivers:: from, content:: [self] ,performative::'refuse' ,protocol:: 'request_supplies'];
	    }
	    else{
	    	do send with: [receivers:: from, content:: [best_dm] ,performative::'inform' ,protocol:: 'request_supplies'];
	    }
	    if (!empty(messages)){
	    	remove index:0 from: messages;
	    }
	}
	
	reflex deliver when: !empty(requester at_distance deliveryRange) // deliveryRange
	{
		ask requester at_distance deliveryRange
		{
			self.target_cell <- self.home_cell;
		}
	}
	
	aspect default {
		draw pyramid(size) at: {location.x,location.y,0} color: color;
		//draw circle(controllerRange) color:rgb(226,126,126);
	}
}

species control parent:building skills:[communicating]
{
	list<supplies> allsupplies;
	rgb color <- rgb("black");
	
	init
	{
		write 'supply control center '+string(self) +' started';
	}
	reflex handle_request when: (!empty(messages)) {
		write 'supply control center '+string(self)+' received a message';
	    message requestfromrequester <- messages at 0;
	    supplies best_supplies <- allsupplies closest_to requestfromrequester.sender;
	    do send with: [receivers:: best_supplies, content:: [requestfromrequester.sender] ,performative::'request' ,protocol:: 'request_supplies'];
	    if (!empty(messages)){
	    	remove index:0 from: messages;
	    }
	}
	
	aspect default {
		draw pyramid(size) at: {location.x,location.y,0} color: color;
		//draw circle(controllerRange) color:rgb(226,126,126);
	}
}

species meetingpoint 
{
	rgb color;
	point mylocation;
	
	aspect default{
		if(mylocation != nil){
			draw sphere(1.5 * people_size) at: mylocation color: color;
			}
	}
}

grid cell width: mapSize_X height: mapSize_Y  neighbours: 8 frequency: 0 {
	bool is_obstacle <- false;
	bool is_supply <- false;
	bool is_free <- true;
	rgb color <- rgb("white");
}

experiment main type: gui {
	parameter "number of camp" var: nb_camp min: 1 max: 1000;
	parameter "number of deliveryman" var: nb_deliveryman min: 1 max: 1000;
	parameter "number of supply station" var: nb_supplies min: 1 max: 1000;
	parameter "scenario type" var: scenario_type min:1 max:6 <- 1;
	parameter "restriction factor" var: restrictionFactor min: 0.0 max: 1.0 <- 0.5;
	
	/*parameter "my_string" var: c <- "" category:"Simple types";
	parameter "my_list" var: d <- [] category:"Complex types";
	parameter "my_matrix" var: e <- matrix([[1,2],[3,4]]) category:"Complextypes";
	parameter "my_pair" var: f <- 3::5 category:"Complex types";
	parameter "my_map" var: g <- ["a"::4] category:"Complex types";
	parameter "my_color" var: h <- #green category:"Complex types";
	parameter "Moving obstacles?" var: a <- true;
	parameter "Moving obstacles?" var: b <- [1,2,3,4];*/
	output {
		
		// Change 120 to daylight_hour to use day/night system
		display map type: opengl ambient_light: 120// daylight /*camera_pos: {world.location.x,-world.shape.height*1.5,70} camera_look_pos:{world.location.x,0,0}*/    
		{
			//image '../images/ground.jpg';
			//grid cell lines: #red;
			species obsticle refresh: false;
			species camp refresh: false;
			species supplies refresh: false;
			species requester;
			species deliveryman;
			species meetingpoint;
			species control refresh: false;

		}
		
	}
}



