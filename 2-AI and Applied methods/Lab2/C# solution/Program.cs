using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace STRIPS_Task2
{
    public class PossibleMoves
    {

        public int a;
        public int b;
        public Stack<int> from;
        public Stack<int> to;

        public PossibleMoves(int aBlockA, int aBlockB, Stack<int> stackA, Stack<int> stackB)
        {
            this.a = aBlockA;
            this.b = aBlockB;
            this.from = stackA;
            this.to = stackB;
        }
    }

    public class Brain
    {
        public List<PossibleMoves> _memory;

        public Brain()
        {
            _memory = new List<PossibleMoves>();
        }
    }

    class Program
    {
        public static int _countSteps = 0;

        public static Stack<int> _table1;
        public static Stack<int> _table2;
        public static Stack<int> _table3;

        public static bool _IS_FINISHED;

        static void Main(string[] args)
        {
            _IS_FINISHED = false;

            _table1 = new Stack<int>();
            _table2 = new Stack<int>();
            _table3 = new Stack<int>();

            _table3.Push(-1);
            _table2.Push(-1);

            _table1.Push(-1);
            _table1.Push(2);
            _table1.Push(3);
            _table1.Push(4);

            Brain _brain = new Brain();

            while (!reachedGoalState() && _countSteps < 100)
            {
                Console.WriteLine("not acheived - enter step: " + _countSteps.ToString());

                List<PossibleMoves> _moves = new List<PossibleMoves>();
                int _topFromTable1 = _table1.Peek();
                int _topFromTable2 = _table2.Peek();
                int _topFromTable3 = _table3.Peek();

                Console.WriteLine("Top of 1 is" + _topFromTable1.ToString());
                Console.WriteLine("Top of 2 is" + _topFromTable2.ToString());
                Console.WriteLine("Top of 3 is" + _topFromTable3.ToString());


                if(canStackOnTop(_topFromTable1, _topFromTable2))
                {
                    PossibleMoves _move = new PossibleMoves(_topFromTable1, _topFromTable2, _table1, _table2);
                    Console.WriteLine("Added move " + _topFromTable1.ToString() + " from table1 to table2");
                    
                    _moves.Add(_move);
                }
                if (canStackOnTop(_topFromTable1, _topFromTable3))
                {
                    PossibleMoves _move = new PossibleMoves(_topFromTable1, _topFromTable3, _table1, _table3);
                    Console.WriteLine("Added move " + _topFromTable1.ToString() + " from table1 to table3");
                    _moves.Add(_move);
                }
                 // 2
                if (canStackOnTop(_topFromTable2, _topFromTable1))
                {
                    PossibleMoves _move = new PossibleMoves(_topFromTable2, _topFromTable1, _table2, _table1);
                    Console.WriteLine("Added move " + _topFromTable2.ToString() + " from table2 to table1");
                    _moves.Add(_move);
                }
                if (canStackOnTop(_topFromTable2, _topFromTable3))
                {
                    PossibleMoves _move = new PossibleMoves(_topFromTable2, _topFromTable3, _table2, _table3);
                    Console.WriteLine("Added move " + _topFromTable2.ToString() + " from table2 to table3");
                    _moves.Add(_move);
                }
                // 3
                if (canStackOnTop(_topFromTable3, _topFromTable1))
                {
                    PossibleMoves _move = new PossibleMoves(_topFromTable3, _topFromTable1, _table3, _table1);
                    Console.WriteLine("Added move " + _topFromTable3.ToString() + " from table3 to table1");
                    _moves.Add(_move);
                }
                if (canStackOnTop(_topFromTable3, _topFromTable2))
                {
                    PossibleMoves _move = new PossibleMoves(_topFromTable3, _topFromTable2, _table3, _table2);
                    Console.WriteLine("Added move " + _topFromTable3.ToString() + " from table3 to table2");
                    _moves.Add(_move);
                }

                // pick a move// pick a move
                Random _random = new Random();
                int _randomMove = _random.Next(_moves.Count);
                PossibleMoves _pickedMove = new PossibleMoves(_moves[_randomMove].a, _moves[_randomMove].b, _moves[_randomMove].from, _moves[_randomMove].to); //_moves[0];

                Console.WriteLine("Total possible moves " + _moves.Count.ToString());
                Console.WriteLine("Picking move " + _randomMove.ToString());
               
                if(false)
                for (int i = 0; i < _moves.Count; i++)
			    {
                    Console.WriteLine("Checking move from " + _moves[i].a.ToString() + " to " + _moves[i].b.ToString());
                    _pickedMove = _moves[i];
			        // Check if we have done this before
                    for (int j = 0; j < _brain._memory.Count; j++)
                    {
                        Console.WriteLine("Brain" + _brain._memory.Count.ToString());
                        if (_brain._memory[j].a != _pickedMove.a
                            &&
                            _brain._memory[j].b != _pickedMove.b)
                        //if (_brain._memory[j].a != _pickedMove.a)
                        break;

                    }
			    }
                
                // Pick a move
                Console.WriteLine("Picked move from " + _pickedMove.a.ToString() + " to " + _pickedMove.b.ToString());

                _pickedMove.to.Push(_pickedMove.from.Pop());

                // Add to brain
                _brain._memory.Add(_pickedMove);

                _countSteps = _countSteps +1;

            }
            while(reachedGoalState())
            {
                if (!_IS_FINISHED)
                {
                    _IS_FINISHED = true;
                    Console.WriteLine("Done in " + _countSteps.ToString() + " steps");
                        

                    Console.WriteLine("top of stack 1 is" + _table1.Peek().ToString());
                    Console.WriteLine("top of stack 2 is" + _table2.Peek().ToString());
                    Console.WriteLine("top of stack 3 is" + _table3.Peek().ToString());

                    int[] _finalArray = _table3.ToArray();
                    for(int i = 0 ; i < _finalArray.Length;i++)
                    {
                        Console.WriteLine("Seat " + i.ToString() + " has value " + _finalArray[i].ToString());
                    }

                }
            }
            while(true)
            {

            }

        }

        public static int getTopItem(int[] aTable)
        {
            int _index = aTable.Length;
            if (_index == 0) 
                return -1; 
            return aTable[_index - 1];
        }

        public static void move(int[] aFrom, int[] aTo)
        {
            // get top positions
            int _countA = aFrom.Length;
            int _countB = aTo.Length;


        }

        public static bool canStackOnTop(int a, int b)
        {
            return a > b;
        }

        public static bool reachedGoalState()
        {
            return (_table3.Count == 4);// && _table3[1] == 3 && _table3[2] == 2);
        }

        
    }
}
