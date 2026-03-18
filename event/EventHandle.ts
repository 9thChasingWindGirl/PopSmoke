import { EventType, EventPayload, EventHandler, Subscription, SubscribeOptions, LogLevel, EventCategory } from './EventType';

interface HandlerEntry {
  handler: EventHandler;
  options: SubscribeOptions;
}

class EventHandle {
  private static instance: EventHandle;
  private handlers: Map<string, HandlerEntry[]> = new Map();
  private eventHistory: EventPayload[] = [];
  private maxHistorySize = 1000;

  private constructor() {}

  public static getInstance(): EventHandle {
    if (!EventHandle.instance) {
      EventHandle.instance = new EventHandle();
    }
    return EventHandle.instance;
  }

  public subscribe(
    eventType: string,
    handler: EventHandler,
    options: SubscribeOptions = {}
  ): Subscription {
    if (!this.handlers.has(eventType)) {
      this.handlers.set(eventType, []);
    }

    const handlers = this.handlers.get(eventType)!;
    handlers.push({ handler, options });
    handlers.sort((a, b) => (b.options.priority || 0) - (a.options.priority || 0));

    return {
      unsubscribe: () => {
        const index = handlers.findIndex(h => h.handler === handler);
        if (index > -1) {
          handlers.splice(index, 1);
        }
      }
    };
  }

  public once(eventType: string, handler: EventHandler): Subscription {
    return this.subscribe(eventType, handler, { once: true });
  }

  public publish(event: EventPayload): void {
    this.eventHistory.push(event);
    if (this.eventHistory.length > this.maxHistorySize) {
      this.eventHistory = this.eventHistory.slice(-this.maxHistorySize);
    }

    const handlers = this.handlers.get(event.type) || [];
    const toRemove: EventHandler[] = [];

    handlers.forEach(({ handler, options }) => {
      try {
        handler(event);
        if (options.once) {
          toRemove.push(handler);
        }
      } catch (error) {
        console.error(`Event handler error for ${event.type}:`, error);
      }
    });

    toRemove.forEach(handler => {
      const index = handlers.findIndex(h => h.handler === handler);
      if (index > -1) {
        handlers.splice(index, 1);
      }
    });
  }

  public publishBatch(events: EventPayload[]): void {
    events.forEach(event => this.publish(event));
  }

  public route(event: EventPayload): void {
    this.publish(event);
  }

  public getEventHistory(): EventPayload[] {
    return [...this.eventHistory];
  }

  public getLogHistory(): EventPayload[] {
    return this.eventHistory.filter(e => 
      e.category === 'session' || 
      e.category === 'auth' || 
      e.category === 'sync' ||
      e.category === 'storage'
    );
  }

  public clearHistory(): void {
    this.eventHistory = [];
  }

  public clearLogs(): void {
    this.eventHistory = this.eventHistory.filter(e => 
      e.category !== 'session' && 
      e.category !== 'auth' && 
      e.category !== 'sync' &&
      e.category !== 'storage'
    );
  }

  public clearSubscription(eventType?: string): void {
    if (eventType) {
      this.handlers.delete(eventType);
    } else {
      this.handlers.clear();
    }
  }

  public hasSubscriber(eventType: string): boolean {
    const handlers = this.handlers.get(eventType);
    return handlers ? handlers.length > 0 : false;
  }

  public getSubscriberCount(eventType: string): number {
    const handlers = this.handlers.get(eventType);
    return handlers ? handlers.length : 0;
  }
}

export { EventHandle };
export default EventHandle.getInstance();
