PImage img;

void setup() {
  img = loadImage("trees.jpg");
  size(img.width, img.height);
  process(img);
  image(img, 0, 0);
  save("processed_trees.png");
}

void process(PImage img) {
  int threshold = color(70, 114, 29);
  for (int x = 0; x < img.width; x++) {
    for (int y = 0; y< img.height; y++) {
      int pos = x + y*width;
      float difference = abs(hue(img.pixels[pos]) - hue(threshold));
      difference = min(difference, -difference + 360.0f);
      //float downward_difference = hue(lower_threshold) - hue(img.pixels[pos]);
      float h = hue(img.pixels[pos]);
      float s = saturation(img.pixels[pos]);
      float b = brightness(img.pixels[pos]);
      colorMode(HSB);
      float saturation_scale = (difference/180.0f)*100.0f;
      //difference = (1.0f-step(difference,30.0f), 0.0f, 1.0f))*s;
      //img.pixels[pos] = color(h,difference,b);
      if ((difference > 30.0f)) { //grayscale
        img.pixels[pos] = color(h,0.0f,b);
      }
      else { //colored
        color c = color(h, s, b);
        img.pixels[pos] = c;
      }
      colorMode(RGB);
    }
  }
}
