package com.example.data.sketchfab

enum class SearchCategory(
    val id: String,
    val displayName: String,
    val targetFolder: String,
    val suggestedPrefabPrefix: String,
    val searchTerms: List<String>
) {
    METRO_CITY(
        id = "metro_city",
        displayName = "A. Metro City",
        targetFolder = "Assets/_Game/Art/Environment/MetroCity",
        suggestedPrefabPrefix = "PF_Metro",
        searchTerms = listOf(
            "realistic metro train",
            "subway train 3D",
            "modern railway station",
            "underground train station",
            "railway platform",
            "railway tracks",
            "metro tunnel",
            "train carriage",
            "railway signal",
            "city street environment",
            "urban buildings",
            "traffic barriers",
            "street lamps",
            "railway crossing",
            "construction equipment"
        )
    ),
    ANCIENT_TEMPLE(
        id = "ancient_temple",
        displayName = "B. Ancient Temple",
        targetFolder = "Assets/_Game/Art/Environment/AncientTemple",
        suggestedPrefabPrefix = "PF_Temple",
        searchTerms = listOf(
            "ancient temple environment",
            "abandoned temple",
            "ancient stone temple",
            "jungle temple",
            "temple ruins",
            "ancient statues",
            "stone bridge",
            "ancient pillars",
            "temple entrance",
            "ancient gates",
            "temple stairs",
            "ancient architecture",
            "jungle ruins",
            "ancient stone traps"
        )
    ),
    JUNGLE(
        id = "jungle",
        displayName = "C. Jungle",
        targetFolder = "Assets/_Game/Art/Environment/Jungle",
        suggestedPrefabPrefix = "PF_Jungle",
        searchTerms = listOf(
            "realistic jungle environment",
            "tropical forest",
            "jungle trees",
            "palm trees",
            "tropical plants",
            "waterfalls",
            "jungle rocks",
            "river environment",
            "wooden bridges",
            "jungle ruins",
            "giant tree roots"
        )
    ),
    FUTURISTIC_CITY(
        id = "futuristic_city",
        displayName = "D. Futuristic City",
        targetFolder = "Assets/_Game/Art/Environment/CyberCity",
        suggestedPrefabPrefix = "PF_Cyber",
        searchTerms = listOf(
            "cyberpunk city",
            "futuristic skyscraper",
            "futuristic train",
            "neon city environment",
            "sci-fi railway station",
            "futuristic tunnel",
            "holographic advertisement",
            "sci-fi buildings",
            "futuristic road"
        )
    ),
    PLAYER_CHARACTERS(
        id = "player_characters",
        displayName = "E. Player Characters",
        targetFolder = "Assets/_Game/Art/Characters",
        suggestedPrefabPrefix = "PF_Character",
        searchTerms = listOf(
            "stylized male game character",
            "stylized female game character",
            "animated runner character",
            "rigged human character",
            "game ready character",
            "low poly animated character",
            "adventure character",
            "running character"
        )
    ),
    OBSTACLES(
        id = "obstacles",
        displayName = "F. Obstacles",
        targetFolder = "Assets/_Game/Art/Obstacles",
        suggestedPrefabPrefix = "PF_Obstacle",
        searchTerms = listOf(
            "rolling stone",
            "ancient boulder",
            "swinging axe",
            "fire trap",
            "stone pillar",
            "railway barrier",
            "road barricade",
            "construction barrier",
            "train obstacle",
            "falling rocks",
            "wooden obstacle",
            "rotating trap"
        )
    ),
    COLLECTIBLES(
        id = "collectibles",
        displayName = "G. Collectibles",
        targetFolder = "Assets/_Game/Art/Collectibles",
        suggestedPrefabPrefix = "PF_Collectible",
        searchTerms = listOf(
            "gold coin 3D",
            "glowing crystal",
            "collectible gem",
            "treasure chest",
            "magic energy orb",
            "power up object",
            "hoverboard 3D",
            "futuristic shield"
        )
    )
}
