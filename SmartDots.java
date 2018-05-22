import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class SmartDots extends JPanel{
  private Population test;

  public SmartDots(){
    setPreferredSize(new Dimension(600,600));
    setFocusable(true);
    test = new Population(1000);
  }

  public void paint(Graphics g){
    super.paint(g);
    Graphics2D g2 = (Graphics2D) g;
    RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHints(rh);
    g2.setColor(new Color(0xfaebd7));
    g2.fillRect(0,0,this.getSize().width,this.getSize().height);

    //Painting the goal.
    g2.setColor(Color.RED);
    g2.fillOval(300,10,5,5);
    for (Dot dot : test.dots){
      drawDot(g2,dot);
    }
    
    //If there are still live dots, continue updating and repainting.
    if (!test.allDotsDead()){
      test.update();
      try{ Thread.sleep(5); }
      catch (InterruptedException e){ System.out.println("Interrupted."); }
      this.repaint();
    }
    //Otherwise, find the fitness, create a new generation, and mutate.
    else{
      test.calculateFitness();
      test.naturalSelection();
      test.mutateGeneration();
      try{ Thread.sleep(500); }
      catch (InterruptedException e){ System.out.println("Interrupted."); }
      this.repaint();
    }
  }

  //--------------------------------------------------------------------------------
  //Method to draw each dot.
  private void drawDot(Graphics g, Dot dot){
    g.setColor(Color.BLACK);
    if (dot.isBest){ g.setColor(Color.GREEN); }
    g.fillOval(dot.x,dot.y,5,5);
  }

  //================================================================================

  static class Population{
    Dot[] dots;
    double fitnessSum;
    int gen = 1;
    int bestDot = 0;
    int minStep = Integer.MAX_VALUE;

    //------------------------------------------------------------------------------
    //If no size is given, the constructor defaults to 500 dots.
    public Population(){
      dots = new Dot[500];
      for (int i = 0; i < 500; i++){ dots[i] = new Dot(); }
    }

    public Population(int size){
      dots = new Dot[size];
      for (int i = 0; i < size; i++){ dots[i] = new Dot(); }
    }

    //------------------------------------------------------------------------------
    //Updates all of the dots.
    public void update(){
      for (Dot dot : dots){
        if (dot.brain.step > minStep){ dot.dead = true; }
        else{ dot.update(); }
      }
    }

    //------------------------------------------------------------------------------
    //Calculates the fitness for each dot.
    public void calculateFitness(){
      for (Dot dot : dots){ dot.calculateFitness(); }
    }

    //------------------------------------------------------------------------------
    //Checks whether all dots are dead.
    public boolean allDotsDead(){
      for (Dot dot : dots){
        //If a dot is alive and it hasn't reached the goal, then not all dots
        //are dead. If a dot is dead, or if the dot has reached the goal, then
        //this statement is skipped for this dot.
        if (!dot.dead && !dot.reachedGoal){ return false; }
      }
      return true;
    }

    //------------------------------------------------------------------------------
    //Gets the next generation of dots.
    public void naturalSelection(){
      Dot[] newDots = new Dot[dots.length];
      setBest();

      //Calculates the sum of the fitnesses.
      fitnessSum = 0;
      for (Dot dot : dots){ fitnessSum += dot.fitness; }

      //Fills the new dot array.
      newDots[0] = dots[bestDot].cloneDot();
      newDots[0].isBest = true;
      for (int i = 1; i < newDots.length; i++){
        Dot parent = selectParent();
        newDots[i] = parent.cloneDot();
      }

      //Sends the best dot to the end (that way it gets rendered on top).
      Dot temp = newDots[0];
      newDots[0] = newDots[newDots.length - 1];
      newDots[newDots.length - 1] = temp;

      for (int i = 0; i < newDots.length; i++){ dots[i] = newDots[i]; }
      gen++;
    }

    //------------------------------------------------------------------------------
    //Finds the best dot (i.e., dot with the highest fitness).
    public void setBest(){
      double max = 0;
      int maxInd = 0;
      for (int i = 0; i < dots.length; i++){
        if (dots[i].fitness > max){
          max = dots[i].fitness;
          maxInd = i;
        }
      }
      bestDot = maxInd;
      if (dots[bestDot].reachedGoal){
        minStep = dots[bestDot].brain.step;
        System.out.println("Fewest steps taken: " + minStep);
      }
    }

    //------------------------------------------------------------------------------
    //Selects a parent at random.
    public Dot selectParent(){
      double limit = Math.random() * fitnessSum;
      double runningSum = 0;
      for (Dot dot : dots){
        runningSum += dot.fitness;
        if (runningSum > limit){ return dot; }
      }
      return null;
    }

    //------------------------------------------------------------------------------
    //Mutates the next generation.
    public void mutateGeneration(){
      for (int i = 0; i < dots.length - 1; i++){ dots[i].brain.mutate(); }
    }
  }

  //================================================================================

  static class Dot{
    int x;
    int y;
    Brain brain;
    boolean dead = false;
    boolean reachedGoal = false;
    boolean isBest = false;
    double fitness = 0;

    //------------------------------------------------------------------------------
    public Dot(){
      brain = new Brain(1000);
      x = 300;
      y = 490;
    }

    //------------------------------------------------------------------------------
    //Updates the position of the dot and checks for collisions.
    public void update(){
      if (!dead && !reachedGoal){
        move();
        if (x < 2 || y < 2 || x > 598 || y > 558){ dead = true; }
        else if (Math.hypot(x - 300, y - 10) < 5){ reachedGoal = true; }
      }
    }

    //------------------------------------------------------------------------------
    //Moves the dot based on the next move in the brain.
    public void move(){
      if (brain.directions.length > brain.step){
        switch (brain.directions[brain.step]){
          case 0: this.y--;
          case 1: this.y--; this.x++;
          case 2: this.x++;
          case 3: this.x++; this.y++;
          case 4: this.y++;
          case 5: this.y++; this.x--;
          case 6: this.x--;
          case 7: this.x--; this.y--;
        }
        brain.step++;
      }else{ dead = true; }
    }

    //------------------------------------------------------------------------------
    //Calculates the fitness of the dot.
    public void calculateFitness(){
      if (reachedGoal){ fitness = 10000.0 / (double) (brain.step * brain.step); }
      else{ fitness = 1.0/Math.pow(Math.hypot(x-300,y-10),2); }
    }

    //------------------------------------------------------------------------------
    //Clones the dot.
    public Dot cloneDot(){
      Dot next = new Dot();
      next.brain = this.brain.cloneBrain();
      return next;
    }
  }

  //================================================================================
  
  static class Brain{
    int[] directions;
    int step = 0;

    //------------------------------------------------------------------------------
    public Brain(int size){
      directions = new int[size];
      Random dir = new Random();
      for (int i = 0; i < size; i++){ directions[i] = dir.nextInt(8); }
    }

    //------------------------------------------------------------------------------
    //Clones the brain.
    public Brain cloneBrain(){
      Brain next = new Brain(this.directions.length);
      for (int i = 0; i < this.directions.length; i++){ next.directions[i] = this.directions[i]; }
      return next;
    }

    //------------------------------------------------------------------------------
    //Mutates the brain.
    public void mutate(){
      double rate = 0.1;
      Random newDir = new Random();
      for (int i = 0; i < this.directions.length; i++){
        if (Math.random() < rate){ this.directions[i] = newDir.nextInt(8); }
      }
    }
  }

  //================================================================================

  public static void main(String[] args){
    JFrame dots = new JFrame();
    dots.setTitle("Smart Dots");
    dots.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    dots.setSize(600,600);
    dots.setResizable(false);
    dots.add(new SmartDots());
    dots.setLocationRelativeTo(null);
    dots.setVisible(true);
  }
}
