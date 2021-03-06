package net.jfabricationgames.gdx.event;

public enum EventType {
	
	//*******************************
	//*** General
	//*******************************
	GAME_START, //
	GAME_LOADED, //
	MAP_ENTERED, //
	SHOW_IN_GAME_SHOP_MENU, //
	START_CUTSCENE, //
	ON_SCREEN_TEXT_ENDED, //
	CHANGE_MAP, // 
	QUICKSAVE, //
	//*******************************
	//*** Player Events
	//*******************************
	PLAYER_RESPAWNED, //
	OUT_OF_AMMO, //
	TAKE_PLAYERS_COINS, //
	PLAYER_BUY_ITEM, //
	//*******************************
	//*** Game Objects
	//*******************************
	EVENT_OBJECT_TOUCHED, //
	UPDATE_MAP_OBJECT_STATES, //
	STATE_SWITCH_ACTION, //
	OPEN_LOCK, //
	CLOSE_LOCK, //
	//*******************************
	//*** Items
	//*******************************
	EVENT_ITEM_PICKED_UP, //
	SET_ITEM, //
	//*******************************
	//*** Cutscenes
	//*******************************
	CUTSCENE_CONDITION, //
	CUTSCENE_EVENT, //
	CUTSCENE_PLAYER_CHOICE, //
	//*******************************
	//*** Fast Travel
	//*******************************
	FAST_TRAVEL_POINT_REGISTERED, //
	FAST_TRAVEL_POINT_ENABLED, //
	FAST_TRAVEL_TO_MAP_POSITION, //
	//*******************************
	//*** Enemy, NPC, AI
	//*******************************
	NPC_INTERACTION, //
	AI_TEAM_CALL, //
	//*******************************
	//*** Conditions
	//*******************************
	SET_GLOBAL_CONDITION_VALUE,
}
