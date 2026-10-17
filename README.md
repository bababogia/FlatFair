FlatFair by Bogiatzis Alexandros

A mobile application designed to make shared household management more **fair, transparent, and engaging**.

Instead of simply assigning chores, Choreconomy introduces an internal **credit-based economy** where roommates can express their preferences through bids. A game-theoretic allocation system then uses these preferences to distribute household responsibilities as fairly as possible.

The app also combines chore management, shared expenses, announcements, and a social hub in one platform.


*** Features

### Chore Bidding

Roommates can bid internal credits on household chores based on how much they want to avoid or take on a particular task.

The system uses these preferences to calculate a fair allocation rather than relying on static assignments.
Example:
Clean Bathroom
Your bid: 20 credits

Take Out Trash
Your bid: 5 credits

Vacuum
Your bid: 10 credits

The allocation algorithm considers the bids of all roommates and attempts to maximize overall household utility while maintaining fairness.

---

### Credit Economy

Each household has its own internal credit system.

Credits can be:

* Earned
* Spent
* Allocated through chores
* Used for bidding
* Refunded
* Adjusted through household rules

All credit changes are recorded in a transaction history.

> Credits are virtual points and are completely separate from real-world money.

---

### Shared Expenses

Roommates can record shared expenses and automatically calculate how much each participant owes.

Supported splitting methods include:

* Equal splitting
* Custom amounts
* Percentage-based splitting

Example:
Groceries
€60.00

Alex     €20.00
Maria    €20.00
Kai     €20.00

The expense system remains separate from the internal credit economy.
---

### Social Hub
A shared household space for communication.

#### Anonymous Board
Roommates can post anonymously about household issues, suggestions, or discussions.

#### Announcements
Important messages can be pinned by household administrators.

Examples:
* Cleaning schedules
* Landlord visits
* House meetings
* Utility reminders

---

### Real-Time Synchronization
Powered by Firebase, the application synchronizes important household information in real time.

This includes:
* Chores
* Bids
* Assignments
* Credit balances
* Credit transactions
* Expenses
* Announcements
* Social posts

---
## Technology Stack
* **Kotlin**
* **Jetpack Compose**
* **Material 3**
* **Kotlin Coroutines**
* **StateFlow**
* **ViewModel**
* **Firebase**


### Allocation Engine

The chore allocation system is implemented as server-side logic using **Node.js/TypeScript Cloud Functions**.

The system is designed around concepts from:

* Utility maximization
* Fair allocation
* Resource allocation
* Strategic bidding
* Game theory

---

## Development Roadmap:
Phase 1 — Foundation
Phase 2 — Authentication & Households
Phase 3 — Chore Management
Phase 4 — Credit Economy
Phase 5 — Bidding System
Phase 6 — Expenses
Phase 7 — Social Hub
Phase 8 — Polish


The objective is not simply to make everyone do the same amount of work.

The objective is to create a system where **different preferences can coexist while the overall household workload remains fair**.

---
## Security

The application treats the Android client as untrusted.
Server-side validation is required for critical operations including:

* Credit balance changes
* Credit transactions
* Chore allocation
* Bid validation
* Expense calculations
* Household membership

Firestore Security Rules ensure that users can only access data belonging to their household.

---


## Future Ideas

Potential future features include:

* AI-assisted chore scheduling
* Household statistics
* Advanced fairness analytics
* Custom household economies
* Reputation systems
* Streaks and achievements
* Seasonal challenges
* Smart recurring chore generation
* Household polls
* Calendar integration
* Wear OS companion
* Multi-household support

