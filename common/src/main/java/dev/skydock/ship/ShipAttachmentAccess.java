package dev.skydock.ship;

/** Per-entity state; a field avoids mixing integrated-server and client entities with the same UUID. */
public interface ShipAttachmentAccess {
    ShipAttachment skydock$attachment();
}
