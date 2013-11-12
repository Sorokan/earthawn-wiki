package repository;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

public class RecentChangeDay {
    LocalDate date;
    List<RecentChange> changes = new ArrayList<>();
}
