import java.io.*;
import java.util.*;

public class Main {

    // ---------- Data classes ----------
    static class Ticket {
        String id;
        String passenger;

        Ticket(String id, String passenger) {
            this.id = id;
            this.passenger = passenger;
        }
    }

    static class Bus {
        String id;
        String route;
        int fare;
        int totalSeats;
        ArrayList<Ticket> booked = new ArrayList<>();
        Queue<String> waiting = new LinkedList<>();   // waiting queue (FIFO) per bus

        Bus(String id, String route, int fare, int totalSeats) {
            this.id = id;
            this.route = route;
            this.fare = fare;
            this.totalSeats = totalSeats;
        }

        int seatsLeft() {
            return totalSeats - booked.size();
        }

        String destination() {
            int i = route.indexOf('-');
            return i < 0 ? route : route.substring(i + 1);
        }
    }

    // ---------- State ----------
    static final String FILE = "buses.txt";
    static ArrayList<Bus> buses = new ArrayList<>();
    static int nextTicket = 101;                       // first ticket ID is TKT101
    static Scanner sc = new Scanner(System.in);

    // ---------- File handling ----------
    static void loadData() {
        File f = new File(FILE);
        if (!f.exists()) {
            System.out.println("Warning: " + FILE + " not found. Starting with no buses.");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#next=")) {
                    nextTicket = Integer.parseInt(line.substring(6).trim());
                    continue;
                }
                String[] p = line.split(",", -1);
                if (p.length < 4) continue;
                Bus b = new Bus(p[0].trim(), p[1].trim(),
                        Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()));
                if (p.length > 4 && !p[4].isEmpty()) {
                    for (String t : p[4].split(";")) {
                        String[] kv = t.split(":", 2);
                        if (kv.length == 2) b.booked.add(new Ticket(kv[0], kv[1]));
                    }
                }
                if (p.length > 5 && !p[5].isEmpty()) {
                    for (String n : p[5].split(";")) b.waiting.add(n);
                }
                buses.add(b);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading " + FILE + ": " + e.getMessage());
        }
    }

    static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println("#next=" + nextTicket);
            for (Bus b : buses) {
                StringBuilder booked = new StringBuilder();
                for (Ticket t : b.booked) {
                    if (booked.length() > 0) booked.append(";");
                    booked.append(t.id).append(":").append(t.passenger);
                }
                pw.println(b.id + "," + b.route + "," + b.fare + "," + b.totalSeats
                        + "," + booked + "," + String.join(";", b.waiting));
            }
        } catch (IOException e) {
            System.out.println("Error saving " + FILE + ": " + e.getMessage());
        }
    }

    // ---------- Features ----------
    static Bus findBus(String id) {                    // linear search by bus ID
        for (Bus b : buses) {
            if (b.id.equalsIgnoreCase(id)) return b;
        }
        return null;
    }

    static void viewBuses() {
        if (buses.isEmpty()) {
            System.out.println("No buses available.");
            return;
        }
        System.out.printf("%-6s %-16s %-6s %-6s %-7s%n", "ID", "Route", "Fare", "Seats", "Waiting");
        for (Bus b : buses) {
            System.out.printf("%-6s %-16s %-6d %-6d %-7d%n",
                    b.id, b.route, b.fare, b.seatsLeft(), b.waiting.size());
        }
    }

    static void searchByDestination() {
        System.out.print("Destination: ");
        String dest = sc.nextLine().trim();
        boolean found = false;
        for (Bus b : buses) {
            if (!dest.isEmpty() && b.destination().equalsIgnoreCase(dest)) {
                System.out.println(b.id + " | " + b.route + " | Fare " + b.fare
                        + " | Seats left " + b.seatsLeft());
                found = true;
            }
        }
        if (!found) System.out.println("No bus found for destination " + dest + ".");
    }

    static void bookTicket() {
        System.out.print("Bus: ");
        String busId = sc.nextLine().trim();
        Bus b = findBus(busId);
        if (b == null) {
            System.out.println("Bus " + busId + " not found.");
            return;
        }
        System.out.print("Passenger: ");
        String name = cleanName(sc.nextLine());
        if (name.isEmpty()) {
            System.out.println("Passenger name cannot be empty.");
            return;
        }
        if (b.seatsLeft() > 0) {
            Ticket t = new Ticket("TKT" + nextTicket++, name);
            b.booked.add(t);
            System.out.println("Ticket booked. " + t.id + " | " + t.passenger + " | " + b.id + " " + b.route);
            System.out.println("Seats left: " + b.seatsLeft());
        } else {
            b.waiting.add(name);
            System.out.println("Bus full. " + name + " added to waiting queue.");
        }
    }

    static void cancelTicket() {
        System.out.print("Ticket: ");
        String tid = sc.nextLine().trim();
        for (Bus b : buses) {
            for (int i = 0; i < b.booked.size(); i++) {
                if (b.booked.get(i).id.equalsIgnoreCase(tid)) {
                    String cancelledId = b.booked.get(i).id;
                    b.booked.remove(i);
                    if (!b.waiting.isEmpty()) {
                        String next = b.waiting.poll();
                        Ticket t = new Ticket("TKT" + nextTicket++, next);
                        b.booked.add(t);
                        System.out.println("Ticket " + cancelledId + " cancelled. Seat given to "
                                + next + ": " + t.id);
                    } else {
                        System.out.println("Ticket " + cancelledId + " cancelled. Seats left: " + b.seatsLeft());
                    }
                    return;
                }
            }
        }
        System.out.println("Ticket " + tid + " not found.");
    }

    static void sortByFare() {
        ArrayList<Bus> sorted = new ArrayList<>(buses);   // copy, keep original order
        for (int i = 0; i < sorted.size() - 1; i++) {     // bubble sort, ascending fare
            for (int j = 0; j < sorted.size() - 1 - i; j++) {
                if (sorted.get(j).fare > sorted.get(j + 1).fare) {
                    Bus tmp = sorted.get(j);
                    sorted.set(j, sorted.get(j + 1));
                    sorted.set(j + 1, tmp);
                }
            }
        }
        for (Bus b : sorted) {
            System.out.println(b.id + " | " + b.fare);
        }
    }

    // remove characters that would break the file format
    static String cleanName(String s) {
        return s.replaceAll("[,:;]", "").trim();
    }

    static void printMenu() {
        System.out.println();
        System.out.println("===== BUS RESERVATION SYSTEM =====");
        System.out.println("1. View buses");
        System.out.println("2. Search by destination");
        System.out.println("3. Book ticket");
        System.out.println("4. Cancel ticket");
        System.out.println("5. Sort by fare");
        System.out.println("6. Exit");
        System.out.print("Choose option: ");
    }

    public static void main(String[] args) {
        loadData();
        boolean running = true;
        while (running) {
            printMenu();
            if (!sc.hasNextLine()) break;                 // end of input
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": viewBuses(); break;
                case "2": searchByDestination(); break;
                case "3": bookTicket(); break;
                case "4": cancelTicket(); break;
                case "5": sortByFare(); break;
                case "6": running = false; break;
                default: System.out.println("Invalid option. Enter 1-6.");
            }
        }
        saveData();
        System.out.println("Data saved. Goodbye!");
    }
}