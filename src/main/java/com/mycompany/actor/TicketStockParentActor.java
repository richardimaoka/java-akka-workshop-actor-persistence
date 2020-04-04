package com.mycompany.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.*;

public class TicketStockParentActor {
  /********************************************************************************
   *  Actor Behaviors
   *******************************************************************************/
  // public: the only Behavior factory method accessed from outside the actor
  public static Behavior<Command> create(){
    return Behaviors.setup(context -> behavior(context, new State()));
  }

  // private: never accessed from outside the actor
  private static Behavior<Command> behavior(ActorContext<Command> context, State state) {
    return Behaviors.receive(Command.class)
      .onMessage(CreateTicketStock.class, command -> behavior(context, spawnTicketStockChild(context, state, command)))
      .onMessage(ProcessOrder.class, command -> behavior(context, forwardProcessOrderToChild(state, command)))
      .build();
  }

  //side effects and return new state
  private static State spawnTicketStockChild(ActorContext<Command> context, State state, CreateTicketStock command) {
    var child = context.spawn(TicketStockActor.create(command.ticketId), Integer.toString(command.ticketId));
    child.tell(new TicketStockActor.CreateTicketStock(command.ticketId, command.quantity));
    return state.put(command.ticketId, child);
  }

  //side effects and return new state
  private static State forwardProcessOrderToChild(State state, ProcessOrder command) {
    var child = state.children.get(command.ticketId);
    if(child == null) {
      System.out.println("bah");
    } else {
      child.tell(new TicketStockActor.ProcessOrder(command.ticketId, command.userId, command.quantity, command.sender));
    }
    return state; //no change in state
  }

  /********************************************************************************
   * Command and State
   *******************************************************************************/
  public interface Command {}
  public static final class CreateTicketStock implements Command {
    public final int ticketId;
    public final int quantity;

    public CreateTicketStock(int ticketId, int quantity) {
      this.ticketId = ticketId;
      this.quantity = quantity;
    }
  }
  public static final class ProcessOrder implements Command {
    public final int ticketId;
    public final int userId;
    public final int quantity;
    public final ActorRef<Object> sender;

    public ProcessOrder(int ticketId, int userId, int quantity, ActorRef<Object> sender) {
      this.ticketId = ticketId;
      this.userId = userId ;
      this.quantity = quantity;
      this.sender = sender;
    }
  }

  public static final class State {
    // There is no Java Map interface which represents immutability.
    // So you have to be careful yourself, to only assign immutable map to `children`.
    public Map<Integer, ActorRef<TicketStockActor.Command>> children;

    public State() {
      children = Map.of(); //immutable
    }

    public State put(int ticketId, ActorRef<TicketStockActor.Command> actorRef) {
      //Need to create a temporary mutable map, as Java has no copy-on-write immutable map unlike Scala
      var copiedMap = new HashMap<>(this.children);
      copiedMap.put(ticketId, actorRef);

      var state = new State();
      state.children = Map.copyOf(copiedMap); //immutable!
      return state;
    }
  }


}
