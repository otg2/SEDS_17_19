/**
 *  Agentvisualalgorithms
 *  Author: ottarg and Xin
 *  Description: 
 */

model Agentvisualalgorithms

global {
	file building_shapefile <- file("../includes/building.shp");
	geometry shape <- envelope(building_shapefile);
	int max_memory <- 5;
	
	int mapSize_X <- 150;
	int mapSize_Y <- 150;
	int controllerRange <- 40;
	
	float people_size <- 2.0;
	float supplies_size <- 5.0;
	float camp_size <- 8.0;
	
	int nb_people <- 3;
	int nb_supplies <- 1;
	int nb_camp <- 1;
	
	cell target_cell;
	point target_point_avg; 
	point target_point_weigh;
	point target_point_weigh_traverse_speed;
	point target_point_weigh_traverse_weigh;
	point target_point_traverse;
	point target_point_norm;
	init 
	{
		// STATIC OBJECTS
		// Create the buildings
		create building from: building_shapefile
		{
			ask cell overlapping self {
				is_obstacle <- true;
				color <- rgb("black");
			}
		}
		
		// Get all available cells
		list<cell> free_cell <- cell where not (each.is_obstacle);//and each.is_obstacle) ;
		
		create people number: nb_people 
		{
			current_cell <- one_of(free_cell);
			//target_cell <- {0,0,0};
			current_cell.is_free <- false;
			remove current_cell from: free_cell;
			location <- current_cell.location;
			
			id <- rnd(1000);
			
			speed <- float(rnd(2) + 1);
			
			// Find first target cell
			memory << current_cell;
			
		}
		
		
		list<people> allLocations <- list<people>(people);
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
		
		write total_factors;
		float avg_x <- 0.0;
		float avg_y <- 0.0;
		
		float avg_x_weigh <- 0.0;
		float avg_y_weigh <- 0.0;
		
		float avg_x_norm <- 0.0;
		float avg_y_norm <- 0.0;
		
		loop p over:allLocations
		{
			avg_x <- avg_x + (p.location.x);
			avg_y <- avg_y + (p.location.y);
			avg_x_weigh <- avg_x_weigh + (p.location.x)* weight(p.speed, total_speed, total_factors);
			avg_y_weigh <- avg_y_weigh + (p.location.y)* weight(p.speed, total_speed, total_factors);
			avg_x_norm <- avg_x_norm + (p.location.x)* normalize(p.speed,max_speed,min_speed) ;
			avg_y_norm <- avg_y_norm + (p.location.y)* normalize(p.speed,max_speed,min_speed);
		}
		
		// Avg # red
		avg_x <- avg_x/n;
		avg_y <- avg_y/n;
		target_point_avg <- {avg_x,avg_y,0};
		
		// normalized #purple
		avg_x_norm <- avg_x_norm/n;
		avg_y_norm <- avg_y_norm/n;
		target_point_norm <- {avg_x_norm,avg_y_norm,0};
		
		// weigh # green
		target_point_weigh <- {avg_x_weigh,avg_y_weigh,0};
		
		// weigh traverse #blue
		target_point_weigh_traverse_speed <- findMinDistance(target_point_weigh, allLocations, total_speed, total_factors,true);
		target_point_weigh_traverse_weigh <- findMinDistance(target_point_weigh, allLocations, total_speed, total_factors,false);
		
		// traverse #black
		target_point_traverse <- find_midpoint(allLocations);
		
		// Set the agent target
		target_cell <- cell closest_to target_point_weigh;
	}
	
	point findMinDistance(point aPoint, list<people> aPeople, float total_speed, float total_factors, bool method)
	{
		point closestPoint <- aPoint;
		float lowestTime <- 100000.0;
		write "enter min distance, current point";
		write string(closestPoint.x) + "," + string(closestPoint.y);
		// try it 50 times
		loop i from:0 to:50
    	{
    		loop x from:closestPoint.x-1 to:closestPoint.x+1
	    	{
	    		loop y from:closestPoint.y-1 to:closestPoint.y+1
		    	{
		    		float newtotaltime <- 0;
					loop p over:aPeople
					{
						float tp <- 0.0;
						if(method)
						{
							tp <- sqrt((x-p.location.x)^2+(y-p.location.y)^2)/p.speed;
						}
						else
						{
							tp <- sqrt((x-p.location.x)^2+(y-p.location.y)^2) * weight(p.speed, total_speed, total_factors);
						}
						///p.speed;
						newtotaltime <- newtotaltime + tp;
					}
					if (newtotaltime<lowestTime)
			        {
			        	write "new time found";
			        	write lowestTime;
			        	closestPoint <- {x,y,0};
			        	write string(closestPoint.x) + "," + string(closestPoint.y);
					    lowestTime <-newtotaltime;
					    // Used to create a visual point
					    create checkRecursion number: 1 
						{
							aMethod <- method;
							location <- closestPoint;
							id <- i;
						}
			        }
		    	}
	    	}
    	}
		return closestPoint;
	}
	
	point find_midpoint(list<people> aPeople)
	{
		int max_x <- 0;
    	int max_y <- 0;
    	int min_x <- 10000;
    	int min_y <- 10000;
    	
    	loop p over:aPeople
		{
			max_x <- int(max([max_x, p.location.x]));
			max_y <- int(min([max_y, p.location.y]));
			min_x <- int(max([min_x, p.location.x]));
			min_y <- int(min([min_y, p.location.y]));
		}
    	point midpoint <- {min_x,min_y,0};
    	float correctness <- 5;
    	
    	float totaltime <- 0;
    	loop p over:aPeople
		{
			totaltime <- totaltime + sqrt((midpoint.x-p.location.x)^2+(midpoint.y-p.location.y)^2)/p.speed;
		}
		
    	loop i from:min_x to:max_x
    	{
    		loop j from:min_y to:max_y
    		{
    			float newtotaltime <- 0;
    			float maxTime <- 0.0;
    			float minTime <- 10000.0;
    			loop p over:aPeople
				{
					float tp <- sqrt((i-p.location.x)^2+(j-p.location.y)^2)/p.speed;
					newtotaltime <- newtotaltime + tp;
					maxTime <- float(max([maxTime, tp]));
					minTime <- float(min([minTime, tp]));
				}
    	        if ((maxTime - minTime) < 50 and newtotaltime<totaltime)
    	        {
    	        	midpoint <- {i,j,0};
    			    totaltime <-newtotaltime;
    	        }
    		}
    		
    	}
    	write "total time";
        write totaltime;
    	
    	return midpoint;
    }
    

	
	
	float weight(float x, int totalSpeed, float totalFactor)
	{
		//write 1-float((x - min)/(max-min));
		return  (totalSpeed/x) / totalFactor;
	}
	
	float normalize(float x, int max, int min)
	{
		//return float(float(speed / max)/float(min/max));
		write x;
		//write 1-float((x - min)/(max-min));
		return 1-float((x - min)/(max-min));
	}
	
}



species building 
{
	float height <- 3.0 + rnd(5);
	aspect default 
	{
		draw shape color: rgb("gray") depth: height;
	}
}


species people skills:[moving] {
	int id;
	cell current_cell;
	//cell target_cell;
	list<cell> memory;
	float size <- people_size;
	rgb color <- rgb(rnd(255),rnd(255),rnd(255));
	
	bool travelingToCamps;
	
	init
	{
		write "Agent " + string(id) + " at location";
		write location;
		write "speed " + string(speed);
	}
	
	reflex move 
	{
		list<cell> possible_cells <- current_cell neighbours_at speed where (not (each.is_obstacle) and each.is_free and not (each in memory));
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
	
	aspect default {
		draw pyramid(size*3) color: rgb((speed/6)*255,255,255);
		draw sphere(size/3) at: {location.x,location.y,size} color: color;
	}
}

species checkRecursion skills:[moving]
{
	int id;
	float size <- people_size/2;
	float trans;
	
	bool aMethod;
	
	init
	{
		trans <- float(id/50);
	}
	
	aspect default 
	{
		draw sphere(size) at: {location.x,location.y,size} 
		color: 
		aMethod ? 
		rgb(trans * 128, (1-trans)*128,0)  // green to red
		: 
		rgb(0, (1-trans)*128,trans * 128); // greed to blue
	}
}


grid cell width: mapSize_X height: mapSize_Y  neighbours: 8 frequency: 0 {
	bool is_obstacle <- false;
	bool is_resource <- false;
	bool is_free <- true;
	rgb color <- rgb("white");
}

experiment main type: gui {
	parameter "nb people" var: nb_people min: 1 max: 1000;
	output {
		
		display map type: opengl ambient_light: 150 /*camera_pos: {world.location.x,-world.shape.height*1.5,70} camera_look_pos:{world.location.x,0,0}*/    
		{
			//image '../images/ground.jpg';
			//grid cell lines: #red;
			species building refresh: false;
			species people; 
			species checkRecursion transparency: 0.3 refresh:false;
			graphics "init" refresh:false
			{
				draw sphere(people_size) at: target_point_avg color: rgb("orange");	
				draw sphere(people_size) at: target_point_norm color: rgb("purple");	
				draw sphere(people_size) at: target_point_weigh color: rgb("green");	
				draw sphere(people_size) at: target_point_traverse color: rgb("gray");
				draw sphere(people_size) at: target_point_weigh_traverse_weigh color: rgb("red");
				draw sphere(people_size) at: target_point_weigh_traverse_speed color: rgb("blue");
				
			}
		}
	}
}



