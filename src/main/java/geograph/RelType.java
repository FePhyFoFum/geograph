package geograph;

import org.neo4j.graphdb.RelationshipType;

public enum RelType implements RelationshipType {
	HAS_RECORD,
	IS_CHILDOF,
	IS_LOCATED;
}
