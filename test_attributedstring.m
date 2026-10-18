#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>

int main() {
    @autoreleasepool {
        int width = 800;
        int height = 600;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(NULL, width, height, 8, width * 4, colorSpace, (CGBitmapInfo)kCGImageAlphaPremultipliedLast);
        
        NSGraphicsContext *nsCtx = [NSGraphicsContext graphicsContextWithCGContext:ctx flipped:YES];
        [NSGraphicsContext setCurrentContext:nsCtx];
        
        NSString *html = @"<div style='font-family:sans-serif;padding:20px;background:#ffffff;border-radius:12px;border:1px solid #ddd;'>"
                         "<h1 style='color:#087f6d;'>HealTouch 推拿门诊管理</h1>"
                         "<p style='color:#333;font-size:16px;'>基于 NSAttributedString HTML 渲染测试</p>"
                         "<table border='1' cellpadding='8' style='border-collapse:collapse;width:100%;'>"
                         "<tr style='background:#f0f7f5;'><th>患者姓名</th><th>治疗项目</th><th>金额</th><th>状态</th></tr>"
                         "<tr><td>张三 (男 42岁)</td><td>颈肩推拿 (60分钟)</td><td>¥180.00</td><td style='color:green;'>已支付</td></tr>"
                         "<tr><td>李小宝 (小儿 5岁)</td><td>小儿脾胃推拿 (30分钟)</td><td>¥120.00</td><td style='color:orange;'>待收费</td></tr>"
                         "</table></div>";
        NSData *data = [html dataUsingEncoding:NSUTF8StringEncoding];
        NSDictionary *options = @{NSDocumentTypeDocumentAttribute: NSHTMLTextDocumentType,
                                  NSCharacterEncodingDocumentAttribute: @(NSUTF8StringEncoding)};
        NSAttributedString *attrStr = [[NSAttributedString alloc] initWithData:data options:options documentAttributes:nil error:nil];
        
        // Draw background
        [[NSColor colorWithRed:0.96 green:0.97 blue:0.98 alpha:1.0] setFill];
        NSRectFill(NSMakeRect(0, 0, width, height));
        
        [attrStr drawInRect:NSMakeRect(40, 40, 720, 520)];
        
        CGImageRef image = CGBitmapContextCreateImage(ctx);
        NSBitmapImageRep *rep = [[NSBitmapImageRep alloc] initWithCGImage:image];
        NSData *png = [rep representationUsingType:NSBitmapImageFileTypePNG properties:@{}];
        [png writeToFile:@"test_html_attr.png" atomically:YES];
        NSLog(@"HTML attr test rendered!");
    }
    return 0;
}
