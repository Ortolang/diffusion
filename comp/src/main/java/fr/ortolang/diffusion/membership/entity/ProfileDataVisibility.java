package fr.ortolang.diffusion.membership.entity;

public enum ProfileDataVisibility {
	// Order is important as enum ordinal is used to compare two ProfileDataVisibility
	EVERYBODY,
	FRIENDS,
	NOBODY;
}
