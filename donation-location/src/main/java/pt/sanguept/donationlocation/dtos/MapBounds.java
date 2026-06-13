package pt.sanguept.donationlocation.dtos;

public record MapBounds(double swLat, double swLng, double neLat, double neLng) {
    public MapBounds {
        if (swLat >= neLat) throw new IllegalArgumentException("swLat must be less than neLat");
        if (swLng >= neLng) throw new IllegalArgumentException("swLng must be less than neLng");
    }
}
