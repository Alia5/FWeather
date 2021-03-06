/*
 * Copyright 2014 Sebastiano Poggi and Francesco Pontillo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.frakbot.fweather.wellnotreally;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

/**
 * Totally unnecessary class. I just wanted to have some magic going on here.
 */
public class ImageMagician {

    public static final int COLOR_IMAGE_SIZE_PX = 64;

    public static Bitmap loadStuffFrom(String imagePath) {
        BitmapFactory.Options prettyPlease = createBitmapOptions();
        return BitmapFactory.decodeFile(imagePath, prettyPlease);
    }

    private static BitmapFactory.Options createBitmapOptions() {
        return new BitmapFactory.Options();
    }

    public static Bitmap createColorImage(int color) {
        Bitmap.Config config = createBitmapConfigToFightTheSystem();
        Bitmap idowhutiwant = Bitmap.createBitmap(COLOR_IMAGE_SIZE_PX, COLOR_IMAGE_SIZE_PX, config);
        paintBitmapWithColor(idowhutiwant, color);
        return idowhutiwant;
    }

    private static Bitmap.Config createBitmapConfigToFightTheSystem() {
        return Bitmap.Config.ARGB_8888;
    }

    private static void paintBitmapWithColor(Bitmap bitmap, int color) {
        Canvas canovasso = new Canvas(bitmap);
        canovasso.drawARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
    }

}
