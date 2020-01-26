model building_v2

import "Agent_release_v2_1.gaml"

global {
	//file building_shapefile <- file("../includes/building.shp");
	//geometry shape <- envelope(building_shapefile);
	map<int,float> staffCapacity;

	float supplyStorage <-10000;
	int max_memory <- 5;
	
	// Each building is (200/n) x 20
	int widthX <- 200;
	int widthY <- 20;

	list<insideBuilding> availableInteriors;
	
	init
	{
		loop i from:1 to: nb_resourceType  
	    { 
          add i::100 to: staffCapacity;
        }
	}
}


species insideBuilding {
	int startPositionX;
	int endPositionX;
	point resource_point;
	point enter_point;
	point exit_point;
	point enter_point_draw;
	point exit_point_draw;
	point mid_Point;
	int posAdd;
	float storage;
	
	list<insideCell> availableCells;
	list<staff> availableStaff;
	
	bool isLoading;
	
	rgb lineColor;
	rgb staffColor;
	
	deliveryman loadingDeliveryMan;
	map<int,float> toload;
	
	map<int,resource_storage> resourceStorage;
	
	init
	{
		storage<-supplyStorage;
		isLoading <- false;
		lineColor <- startPositionX = 0 ? #white : #black;
		staffColor <- rgb(rnd(255),rnd(255),rnd(255));
		
		//resource_point <- {startPositionX + posAdd/2, world.location.y};
		enter_point <- {startPositionX+15, world.location.y*2};
		exit_point <- {endPositionX-15, world.location.y*2};
		
		enter_point_draw <- {enter_point.x, enter_point.y, 4};
		exit_point_draw  <- {exit_point.x, exit_point.y, 4};
		// Let them be idle at mid point, for fun
		mid_Point <- {startPositionX + posAdd/2, world.location.y};
		insideCell the_target_cell <- insideCell closest_to mid_Point;
		
		create staff number: 3 
		{
			current_cell <- one_of(myself.availableCells);
			current_cell.is_free <- false;
			remove current_cell from: myself.availableCells;
			location <- current_cell.location;
			target_cell <- the_target_cell;
			memory << current_cell;
			color <- myself.staffColor;
			
			myBuilding <- myself;
		}
		int resource_id<-1;
		create resource_storage number: nb_resourceType returns: rs
		{
			current_cell <- one_of(myself.availableCells);
			current_cell.is_free <- false;
			remove current_cell from: myself.availableCells;
			location <- current_cell.location;
			resourceID <-resource_id;
			resource_id<-resource_id+1;
		}
		loop ars over:rs
		{
			add ars.resourceID::ars to:resourceStorage;
		}		
	}
	//Need to double check the valuable names
	/*reflex register when: loadingDeliveryMan!=nil and isLoading=false
	{
		loop rescID over:loadingDeliveryMan.resourceInfo.keys
		{
			add rescID::(loadingDeliveryMan.resourceInfo[rescID].original_storage-loadingDeliveryMan.resourceInfo[rescID].holdingAmount) to: toload;
		}
		isLoading<-true;
	}
	
	reflex deregister when: isLoading=true
	{
		bool loadingEnd <-true;
		loop aResourceID over:toload.keys
		{
			if toload[aResourceID]>resourceStorage[aResourceID].loaded
			{
				loadingEnd <-false;
			}
		}
		if loadingEnd
		{
			isLoading<-false;
			loadingDeliveryMan<-nil;
			toload<-nil;
			loop aRescID over:resourceStorage.keys
			{
				resourceStorage[aRescID].loaded<-0;
				resourceStorage[aRescID].loading<-0;
				resourceStorage[aRescID].ontheway<-0;
			}
		}
	}*/
	
	aspect default {
		//draw shape color: rgb("gray") depth: height;
		draw sphere(4) at: enter_point_draw color: rgb("green");	
		draw sphere(4) at: exit_point_draw color: rgb("red");	
		
		// Draw wall
		if(startPositionX > 0)
		{
			draw line([{startPositionX, 0}, {startPositionX, world.location.y*2}]) depth:5 color: lineColor;
		}
		
	}
}

species resource_storage
{
	int resourceID;
	insideCell current_cell;
	float storage;
	float loaded;
	float loading;
	float ontheway;
	point resource_draw;
	
	init
	{
		loaded <- 0.0;
		loading<-0.0;
		ontheway<-0.0;
		
		resource_draw <- {location.x, location.y, 4};
	}
	aspect default {
		draw sphere(4) at: resource_draw color: rgb(10*resourceID,10*resourceID,10*resourceID);	
	}	
}

species staff {
	insideCell current_cell;
	insideCell target_cell;
	list<insideCell> memory;
	rgb color;
	insideBuilding myBuilding;
	int speed <- 5;
	
	float carry;
	int resource_ID<-0;
	map<int,float> capacity;
	
	// Too lazy to do FSM
	bool travelToResource;
	bool travelToDelivery;
	
	init
	{
		capacity<-staffCapacity;
	}
	// Just chat yeye
	reflex chat when: !myBuilding.isLoading
	{
		target_cell <- insideCell closest_to myBuilding.mid_Point;
	}	
	// Move to green, someone entered
	reflex collectInfo when: myBuilding.isLoading and resource_ID=0
	{
		//target_cell <- insideCell closest_to myBuilding.enter_point;
		bool infoCollected<-false;
		loop aRescID over:myBuilding.toload.keys 
		{
			if(infoCollected=false and myBuilding.resourceStorage[aRescID].loading<myBuilding.toload[aRescID]){
				infoCollected <- true;
				myBuilding.resourceStorage[aRescID].loading <- myBuilding.resourceStorage[aRescID].loading + capacity[aRescID];
				resource_ID<-aRescID;
				travelToResource<-true;
			}
		}
	}	
	
	// Move to yellow
	reflex getResource when: travelToResource
	{
		target_cell <- insideCell closest_to myBuilding.resourceStorage[resource_ID].location;
		if(self.location distance_to target_cell.location < 2)
		{
			travelToResource <- false;
			travelToDelivery <- true;
			// Update carry 
			carry <-myBuilding.toload[resource_ID]-myBuilding.resourceStorage[resource_ID].ontheway > capacity[resource_ID]? capacity[resource_ID] : myBuilding.toload[resource_ID]-myBuilding.resourceStorage[resource_ID].ontheway;
			myBuilding.resourceStorage[resource_ID].ontheway <-myBuilding.resourceStorage[resource_ID].ontheway+carry;
		}
	}
	
	// Move to green
	reflex deliverResource when: travelToDelivery
	{
		target_cell <- insideCell closest_to myBuilding.exit_point;
		if(self.location distance_to target_cell.location < 2)
		{
			myBuilding.resourceStorage[resource_ID].loaded <- myBuilding.resourceStorage[resource_ID].loaded + carry;
			carry <- 0;
			travelToDelivery <- false;
			resource_ID<-0;
		}
	}
	
	reflex move_1 when:((current_cell.location distance_to target_cell.location)>speed)
	{
		list<insideCell> possible_cells <- current_cell neighbours_at speed where (not (each.is_obstacle) and each.is_free and not (each in memory));
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
	
	aspect default {
		draw pyramid(4) color: color;
		draw sphere(4/3) at: {location.x,location.y,4} color: color;
	}
}

grid insideCell width: widthX height: widthY  neighbours: 8 frequency: 0 {
	bool is_obstacle <- false;
	bool is_free <- true;
	rgb color <- rgb("white");
}
