#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>

int main(int argc, const char *argv[]) {
    @autoreleasepool {
        if (argc < 3) {
            NSLog(@"Usage: %s <input.svg> <output.png> [scale]", argv[0]);
            return 1;
        }
        NSString *inPath = [NSString stringWithUTF8String:argv[1]];
        NSString *outPath = [NSString stringWithUTF8String:argv[2]];
        CGFloat scale = (argc >= 4) ? atof(argv[3]) : 1.0;
        
        NSData *svgData = [NSData dataWithContentsOfFile:inPath];
        if (!svgData) {
            NSLog(@"Could not read file: %@", inPath);
            return 1;
        }
        
        NSImage *image = [[NSImage alloc] initWithData:svgData];
        if (!image) {
            NSLog(@"Could not parse SVG data");
            return 1;
        }
        
        NSSize originalSize = [image size];
        NSSize targetSize = NSMakeSize(originalSize.width * scale, originalSize.height * scale);
        
        NSBitmapImageRep *rep = [[NSBitmapImageRep alloc]
            initWithBitmapDataPlanes:NULL
                          pixelsWide:targetSize.width
                          pixelsHigh:targetSize.height
                       bitsPerSample:8
                     samplesPerPixel:4
                            hasAlpha:YES
                            isPlanar:NO
                      colorSpaceName:NSDeviceRGBColorSpace
                        bitmapFormat:0
                         bytesPerRow:0
                        bitsPerPixel:0];
        
        [NSGraphicsContext saveGraphicsState];
        [NSGraphicsContext setCurrentContext:[NSGraphicsContext graphicsContextWithBitmapImageRep:rep]];
        [[NSGraphicsContext currentContext] setImageInterpolation:NSImageInterpolationHigh];
        
        [image drawInRect:NSMakeRect(0, 0, targetSize.width, targetSize.height)
                 fromRect:NSZeroRect
                operation:NSCompositingOperationCopy
                 fraction:1.0];
                 
        [NSGraphicsContext restoreGraphicsState];
        
        NSData *pngData = [rep representationUsingType:NSBitmapImageFileTypePNG properties:@{}];
        [pngData writeToFile:outPath atomically:YES];
        NSLog(@"Successfully rendered SVG %@ -> PNG %@ (%dx%d)", inPath, outPath, (int)targetSize.width, (int)targetSize.height);
    }
    return 0;
}
