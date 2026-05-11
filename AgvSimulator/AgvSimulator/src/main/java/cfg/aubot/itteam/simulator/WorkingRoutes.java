package cfg.aubot.itteam.simulator;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class WorkingRoutes {

  private int mapId;

  private List<Route> routes;

  private Route currentRoute;

  private Map<String, String> pointActions;

  public WorkingRoutes() {
    routes = new ArrayList<>();
    pointActions = new HashMap<>();
  }

  @Data
  @AllArgsConstructor
  public static class Route {
    int id;
    private Map<String, String> pointDirections;
  }
}
