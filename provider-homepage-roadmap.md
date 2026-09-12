# Roadmap: Provider-Driven Home Page & Detail Cards (No TMDB)

This roadmap outlines the step-by-step process to transition your app from a TMDB-first home screen to a **Pure Provider-Driven Home Screen**. It also covers how to handle the detail cards so that clicking an item seamlessly loads its streams directly from the provider.

---

## Phase 1: Core Data Models
Before changing any UI or networking code, we need models that can represent a "Homepage Section" (e.g., "Trending", "Latest Movies").

1. **Create a `HomePageList` Model:**
   ```kotlin
   data class HomePageList(
       val title: String, // e.g., "Trending Now", "Latest Bollywood"
       val items: List<SearchResult> // The items inside this row
   )
   ```
2. **Create a `HomePageResponse` Model:**
   ```kotlin
   data class HomePageResponse(
       val sections: List<HomePageList> // A list of all the rows to show on the screen
   )
   ```

## Phase 2: Updating the Provider Architecture
We need to give providers the ability to fetch their own homepage data and feed it to the app.

1. **Update `Provider.kt` Interface:**
   Add a new function that all plugins can implement to return their homepage layout.
   ```kotlin
   interface Provider {
       // ... existing search and load methods ...
       
       suspend fun getMainPage(): HomePageResponse {
           return HomePageResponse(emptyList()) // Default empty implementation
       }
   }
   ```
2. **Implement in a Provider (e.g., MovieBox or NetMirror):**
   * Open your selected provider (e.g., `MovieBoxProvider.kt`).
   * Override `getMainPage()`.
   * Write a scraper that hits the provider's homepage URL, parses the HTML or JSON, extracts the categories (like "Latest Movies"), converts them into `SearchResult` objects, and returns them wrapped in a `HomePageResponse`.

## Phase 3: Connecting the ViewModel
The `HomeViewModel` will stop asking TMDB for the layout and start asking the currently active Provider.

1. **Refactor `HomeViewModel.kt`:**
   * Completely remove the static TMDB calls (`getTrendingMovies()`, `getPopularTv()`).
   * Add a call to `ProviderRepository.selectedProvider.getMainPage()`.
   * Expose the `sections` directly as a `StateFlow` to the UI.

## Phase 4: Updating the UI (Home & Detail Cards)

### The Home Screen
1. **Dynamic Rows:** Update `HomeScreen.kt` to iterate over `HomePageResponse.sections`. Instead of hardcoded TMDB rows, it will dynamically generate a `LazyRow` for every single section the provider returns.
2. **Basic Imagery:** Since we aren't using TMDB, the posters will come directly from the provider's `SearchResult.poster` URL.

### The Detail Card (Metadata & Streams)
When the user clicks a poster on the new Home Screen, the app opens the Detail Card.

1. **Direct Routing:** 
   Because the clicked item is a `SearchResult` that came directly from the Provider, you already have the correct Provider ID attached to it!
2. **Bypass the Search Step:**
   Normally, the app has to take a TMDB ID, reverse-search it on the provider, and cross your fingers that it matches. 
   Now, you can skip the search step entirely. You pass the `SearchResult` directly into `StreamRepository.loadContent()`. It will instantly load the episodes and streams for that specific item.
3. **Display Provider Data:** 
   The Detail View UI will use the basic details provided by the provider (title, year, basic poster). 

---

> **Tip:** Start small! Pick just one provider to test this on. Write a script to scrape its homepage first before wiring it up to the entire app architecture.
