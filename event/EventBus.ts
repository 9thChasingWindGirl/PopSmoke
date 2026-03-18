import { EventPayload } from './EventType';
import EventHandle from './EventHandle';

type EventBusHandler = (payload: EventPayload) => void;

class EventBus {
  private static instance: EventBus;
  private middlewares: EventBusHandler[] = [];

  private constructor() {}

  public static getInstance(): EventBus {
    if (!EventBus.instance) {
      EventBus.instance = new EventBus();
    }
    return EventBus.instance;
  }

  public use(middleware: EventBusHandler): void {
    this.middlewares.push(middleware);
  }

  public dispatch(event: EventPayload): void {
    let processedEvent = event;

    for (const middleware of this.middlewares) {
      try {
        const result = middleware(processedEvent);
        if (result !== undefined) {
          processedEvent = result;
        }
      } catch (error) {
        console.error('[EventBus] Middleware error:', error);
      }
    }

    EventHandle.publish(processedEvent);
  }

  public clearMiddlewares(): void {
    this.middlewares = [];
  }

  public getMiddlewareCount(): number {
    return this.middlewares.length;
  }
}

export { EventBus };
export default EventBus.getInstance();
