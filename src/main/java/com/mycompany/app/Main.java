package com.mycompany.app;

import akka.actor.typed.*;
import com.mycompany.actor.*;

public class Main {
  public static void main(String[] args) {
    // boot up server using the route as defined below
    var system = ActorSystem.create(TicketStockParentActor.create(), "guardian_ticket_parent");

    system.tell(new TicketStockParentActor.CreateTicketStock(1, 3));
    system.tell(new TicketStockParentActor.ProcessOrder(1, 1, 1, null));
  }
}