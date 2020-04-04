package com.mycompany.app;

import akka.actor.typed.*;
import com.mycompany.actor.*;

public class Main {
  public static void main(String[] args) throws Exception {
    // boot up server using the route as defined below
    var system = ActorSystem.create(TicketStockActor.create(1234556), "guardian_ticket");

    system.tell(new TicketStockActor.CreateTicketStock(1, 3));
    system.tell(new TicketStockActor.ProcessOrder(1, 1, 1, null));
    system.tell(new TicketStockActor.ProcessOrder(1, 1, 1, null));
    system.tell(new TicketStockActor.ProcessOrder(1, 1, 1, null));
    system.tell(new TicketStockActor.ProcessOrder(1, 1, 1, null));
  }
}