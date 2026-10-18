#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>

int main() {
    @autoreleasepool {
        NSString *svg = @"<svg width='400' height='200' xmlns='http://www.w3.org/2000/svg'><rect width='100%' height='100%' fill='#087f6d'/><text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='20' fill='white'>SVG Test</text></svg>";
        NSData *data = [svg dataUsingEncoding:NSUTF8StringEncoding];
        NSImage *img = [[NSImage alloc] initWithData:data];
        if (img) {
            NSLog(@"NSImage loaded SVG! Size: %@", NSStringFromSize(img.size));
            CGImageRef cg = [img CGImageForProposedRect:NULL context:NULL hints:NULL];
            NSBitmapImageRep *rep = [[NSBitmapImageRep alloc] initWithCGImage:cg];
            NSData *png = [rep representationUsingType:NSBitmapImageFileTypePNG properties:@{}];
            [png writeToFile:@"test_svg_out.png" atomically:YES];
            NSLog(@"Saved SVG to PNG!");
        } else {
            NSLog(@"NSImage failed to load SVG");
        }
    }
    return 0;
}
