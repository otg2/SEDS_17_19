/**
 *  CellTets
 *  Author: ottarg
 *  Description: 
 */

model CellTets

/* Insert your model definition here */
global
{
	int numberOfCells <- 100;
	int numberOfWhiteCells <- 5;
	int environmentSize <-1000;
	geometry shape <- cube(environmentSize);	
	int cellRadius <- 10;
	
	init
	{
		create cells number: numberOfCells
		{
			location <- {rnd(environmentSize), rnd(environmentSize), rnd(environmentSize)};
		}
		create whiteCells number: numberOfWhiteCells
		{
			location <- {rnd(environmentSize), rnd(environmentSize), rnd(environmentSize)};
		}
	}
}

species whiteCells skills:[moving3D]
{
	int whiteCellRadius <- 25;
	int detectRadius <- 50;
	list<cells> neighbors;
	cells targetCell;
	
	reflex wander 
	{ 
		targetCell <- one_of (neighbors) ;
		if(!empty(neighbors))
		{
			location <- targetCell.location ;
		}
		else
    	{
    		do wander amplitude: 50 speed: 25;
		}
        		
    }
    
    reflex findCorrupt 
    {
        neighbors <- cells select ((each distance_to self) < detectRadius);
    }  
    
   	reflex cureDisease when: !empty(cells at_distance (whiteCellRadius))
	{
		ask cells at_distance whiteCellRadius
		{
			if(self.is_infected and !is_cured)
			{
				self.is_infected <- true;
				self.is_cured <- true;
			}
		}
	}
	
	aspect default
	{
		draw sphere(whiteCellRadius) color: #white;
	}
}

species cells skills:[moving3D]
{
	bool is_infected <- flip(0.1);
	bool is_cured <- false;
	rgb drawColor;
	
	int amplitude;
	int speed;
	
	init
	{
		amplitude <- is_infected ? 50 : 25;
		speed <- is_infected ? 20 : 10;
	}
	
	reflex wander { 
        do  wander amplitude: amplitude speed: speed; 
    }
	
	
	reflex transferDisease when: !empty(cells at_distance (cellRadius*2))
	{
		ask cells at_distance cellRadius
		{
			if(self.is_infected and !is_cured)
			{
				myself.is_infected <- true;
			}
			else if(myself.is_infected and !is_cured) 
			{
				self.is_infected <- true;
			}
		}
	}
	
	aspect default
	{
		drawColor <- #blue;
		if(is_infected) {drawColor <- #red;}
		if(is_cured) {drawColor <- #orange;}
		
		draw sphere(cellRadius) color: drawColor;
	}
}



experiment Cool3DWorld type: gui
{
	parameter "Set number of cells: " var:numberOfCells min:1 max:1000 category: "Cells";
	output
	{
		display chart
		{
			chart "number of infected cells"
			{
				data "infected cells" value:length(cells where (each.is_infected = true));
				data "cured cells" value:length(cells where (each.is_cured = true));
			}
		}
		display View1 type:opengl background:rgb(10,40,55)
		{
			graphics "env"
			{
				draw cube(environmentSize) color: #black empty:true;
			}
			species cells;
			species whiteCells;
		}
		
	}
}

