
package com.appcloner.presentation.search

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.appcloner.AppInfo

class SmartSearchEngine {

    data class SearchResult(
        val app: AppInfo,
        val matchScore: Float,
        val matchedIn: MatchLocation,
        val highlightedName: Spannable,
        val highlightedPackage: Spannable
    )

    enum class MatchLocation {
        NAME,
        PACKAGE,
        BOTH
    }

    fun search(query: String, apps: List<AppInfo>): List<SearchResult> {
        if (query.isEmpty()) {
            return apps.map { app ->
                SearchResult(
                    app = app,
                    matchScore = 1.0f,
                    matchedIn = MatchLocation.NAME,
                    highlightedName = SpannableString(app.appName),
                    highlightedPackage = SpannableString(app.packageName)
                )
            }
        }

        val queryLower = query.lowercase()
        val results = mutableListOf<SearchResult>()

        apps.forEach { app ->
            val nameLower = app.appName.lowercase()
            val packageLower = app.packageName.lowercase()

            var matchScore = 0f
            var matchedIn = MatchLocation.NAME

            // 精确匹配包名（最高优先级）
            if (packageLower == queryLower) {
                matchScore = 1.0f
                matchedIn = MatchLocation.PACKAGE
            }
            // 包名开头匹配
            else if (packageLower.startsWith(queryLower)) {
                matchScore = 0.9f
                matchedIn = MatchLocation.PACKAGE
            }
            // 包名包含匹配
            else if (packageLower.contains(queryLower)) {
                matchScore = 0.8f
                matchedIn = MatchLocation.PACKAGE
            }
            // 包名模糊匹配
            else if (fuzzyMatch(queryLower, packageLower) > 0.7f) {
                matchScore = fuzzyMatch(queryLower, packageLower) * 0.7f
                matchedIn = MatchLocation.PACKAGE
            }
            // 精确匹配名称
            else if (nameLower == queryLower) {
                matchScore = 1.0f
                matchedIn = MatchLocation.NAME
            }
            // 名称开头匹配
            else if (nameLower.startsWith(queryLower)) {
                matchScore = 0.95f
                matchedIn = MatchLocation.NAME
            }
            // 名称包含匹配
            else if (nameLower.contains(queryLower)) {
                matchScore = 0.85f
                matchedIn = MatchLocation.NAME
            }
            // 名称模糊匹配
            else if (fuzzyMatch(queryLower, nameLower) > 0.6f) {
                matchScore = fuzzyMatch(queryLower, nameLower) * 0.6f
                matchedIn = MatchLocation.NAME
            }

            if (matchScore > 0f) {
                val highlightedName = highlightMatches(app.appName, query)
                val highlightedPackage = highlightMatches(app.packageName, query)
                
                results.add(
                    SearchResult(
                        app = app,
                        matchScore = matchScore,
                        matchedIn = matchedIn,
                        highlightedName = highlightedName,
                        highlightedPackage = highlightedPackage
                    )
                )
            }
        }

        return results.sortedByDescending { it.matchScore }
    }

    fun fuzzyMatch(query: String, target: String): Float {
        if (query.isEmpty() || target.isEmpty()) return 0f
        
        val queryChars = query.toCharArray()
        val targetChars = target.toCharArray()
        
        var queryIndex = 0
        var matchCount = 0
        
        for (char in targetChars) {
            if (queryIndex < queryChars.size && char == queryChars[queryIndex]) {
                matchCount++
                queryIndex++
            }
        }
        
        if (queryIndex < queryChars.size) {
            return 0f
        }
        
        return matchCount.toFloat() / targetChars.size
    }

    fun highlightMatches(text: String, query: String, color: Int = android.graphics.Color.RED): Spannable {
        val spannable = SpannableString(text)
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        
        var startIndex = 0
        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index < 0) break
            
            spannable.setSpan(
                ForegroundColorSpan(color),
                index,
                index + query.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            startIndex = index + 1
        }
        
        return spannable
    }

    fun getSearchSuggestions(query: String, recentSearches: List<String>): List<String> {
        if (query.isEmpty()) {
            return recentSearches.take(5)
        }
        
        val lowerQuery = query.lowercase()
        return recentSearches
            .filter { it.lowercase().contains(lowerQuery) }
            .take(5)
    }

    fun addToRecentSearches(query: String, recentSearches: MutableList<String>, maxSize: Int = 20) {
        if (query.isEmpty()) return
        
        recentSearches.remove(query)
        recentSearches.add(0, query)
        
        while (recentSearches.size > maxSize) {
            recentSearches.removeAt(recentSearches.size - 1)
        }
    }
}
