package com.mycompany.actor;

import akka.actor.typed.*;
import akka.persistence.typed.*;
import akka.persistence.typed.javadsl.*;
import com.mycompany.actor.TicketStockActor.*;

public class TicketStockActor extends EventSourcedBehavior<Command, Event, State> {

  /********************************************************************************
   *  Actor factory
   *******************************************************************************/
  // public: the only Behavior factory method accessed from outside the actor
  public static Behavior<Command> create(int ticketId){
    return new TicketStockActor(PersistenceId.ofUniqueId(Integer.toString(ticketId)));
  }

  private TicketStockActor(PersistenceId persistenceId){
    super(persistenceId);
  }

  @Override
  public State emptyState() {
    return new Initialized();
  }

  /********************************************************************************
   * Persistence
   *******************************************************************************/
  @Override
  public CommandHandler<Command, Event, State> commandHandler(){
    var builder = newCommandHandlerBuilder();

    builder
      .forStateType(Initialized.class)
      .onCommand(CreateTicketStock.class, command -> Effect().persist(new TicketStockCreated(command.ticketId, command.quantity)));

    builder
      .forStateType(Available.class)
      .onCommand(ProcessOrder.class, (state, command) -> {
        var decrementedQuantity = state.quantity - command.quantityDecrementedBy;

        if (state.ticketId != command.ticketId) {
          return Effect().none().thenRun(() -> System.out.println(String.format("wrong ticket id = %d, expected = %d", command.ticketId, state.ticketId)));
        } else if (decrementedQuantity < 0) {
          return Effect().none().thenRun(() -> System.out.println(String.format("you cannot purchase qty = %d, which is more than available qty = %d", command.quantityDecrementedBy, state.quantity)));
        } else if (decrementedQuantity == 0) {
          return Effect().persist(new SoldOut(command.ticketId));
        } else { //decrementedQuantity > 0
          return Effect().persist(new OrderProcessed(command.ticketId, command.quantityDecrementedBy));
        }
      });

    builder
      .forStateType(OutOfStock.class)
      .onCommand(ProcessOrder.class, command -> Effect().reply(null, "out of stock!!!"));

    return builder.build();
  }

  @Override
  public EventHandler<State, Event> eventHandler() {
    var builder = newEventHandlerBuilder();

    builder
      .forStateType(Initialized.class)
      .onEvent(TicketStockCreated.class, (state, event) -> new Available(event.ticketId, event.quantity));

    builder
      .forStateType(Available.class)
      .onEvent(OrderProcessed.class, (state, event) -> new Available(state.ticketId, event.newQuantity))
      .onEvent(SoldOut.class, (state, event) -> new OutOfStock(state.ticketId));

    return builder.build();
  }

  /********************************************************************************
   * Command, Event, and State
   *******************************************************************************/
  public interface Command {}
  public static final class CreateTicketStock implements Command {
    public int ticketId;
    public int quantity;

    public CreateTicketStock(int ticketId, int quantity) {
      this.ticketId = ticketId;
      this.quantity = quantity;
    }
  }
  public static final class ProcessOrder implements Command {
    public int ticketId;
    public int userId;
    public int quantityDecrementedBy;
    public ActorRef<Object> sender;

    public ProcessOrder(int ticketId, int userId, int quantityDecrementedBy, ActorRef<Object> sender) {
      this.ticketId = ticketId;
      this.userId = userId;
      this.quantityDecrementedBy = quantityDecrementedBy;
      this.sender = sender;
    }
  }

  public interface Event {}
  public static final class TicketStockCreated implements Event {
    public int ticketId;
    public int quantity;

    public TicketStockCreated(int ticketId, int quantity) {
      this.ticketId = ticketId;
      this.quantity = quantity;
    }
  }
  public static final class OrderProcessed implements Event {
    public int ticketId;
    public int newQuantity;

    public OrderProcessed(int ticketId, int newQuantity) {
      this.ticketId = ticketId;
      this.newQuantity = newQuantity;
    }
  }
  public static final class SoldOut implements Event {
    public int ticketId;

    public SoldOut(int ticketId) {
      this.ticketId = ticketId;
    }
  }

  public interface State {}
  private final class Initialized implements State {}
  private final class Available implements State {
    public int ticketId;
    public int quantity;

    public Available(int ticketId, int quantity) {
      this.ticketId = ticketId;
      this.quantity = quantity;
    }
  }
  private class OutOfStock implements State {
    public int ticketId;

    public OutOfStock(int ticketId) {
      this.ticketId = ticketId;
    }
  }

}
