//
//  ActivityStarter.swift
//  r2-testapp-swift
//
//  Created by LeeJongMin on 2019/7/28.
//
//  Copyright 2019 European Digital Reading Lab. All rights reserved.
//  Licensed to the Readium Foundation under one or more contributor license agreements.
//  Use of this source code is governed by a BSD-style license which is detailed in the
//  LICENSE file present in the project repository where this source code is maintained.
//

import Foundation
import UIKit

@objc (ActivityStarter) class ActivityStarter: NSObject {
    
    static var app = try! AppModule()
    static var handler : ((Int, Int, String)->Void)?
    static var docIndex = 0
    class func docChanged(_ docIndex: Int) {
        ActivityStarter.docIndex = docIndex
    }
    class func pageChanged(index: Int, total: Int, href: String? = "") {
        var url = href
        if let href = href {
            url = href.components(separatedBy: "OEBPS/")[1].components(separatedBy: ".xhtml")[0]
        }
        handler?(index, total, url!)
    }
    @objc func navigateToReadium(_ filePath: String, handler: @escaping ((Int, Int, String)->Void)) {
        ActivityStarter.handler = handler
        //let filePath = "file://" + filePath
        var filePath = URL(fileURLWithPath: filePath).absoluteString
        print( URL(string: filePath)!)
        //    filePath = "file:///Users/lizhongming/Library/Developer/CoreSimulator/Devices/4E09BFA4-501F-4FCF-B5E7-F1E1627BE7AA/data/Containers/Data/Application/2F3A7744-E150-47FB-AA44-9E20D44A9792/Documents/1.epub";
        print(FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first)
        var books = try! BooksDatabase.shared.books.all()
        for book in books {
            if book.href == URL(string: filePath)?.lastPathComponent {
                open(book)
                return
            }
        }
        
        //app.library.library.movePublicationToLibrary(from: URL(string: filePath)!)
        if ActivityStarter.app.library.library.addPublication(at: URL(string: filePath)!) {
            books = try! BooksDatabase.shared.books.all()
            for book in books {
                if book.href == filePath {
                    open(book)
                    return
                }
            }
        }
        
    }
    func open(_ book: Book) {
        guard let (publication, container) = ActivityStarter.app.library.library.parsePublication(for: book) else {
            return
        }
        
        DispatchQueue.main.async {
            ActivityStarter.app.library.library.preparePresentation(of: publication, book: book, with: container)
            
            if let topVC = UIApplication.topViewController() {
                
                //      self.app.reader.presentPublication(publication: publication, book: book, in: UINavigationController(rootViewController: topVC), completion: {})
                ActivityStarter.app.reader.presentPublication(publication: publication, book: book, in: nil, completion: {})
            }
        }
        
    }
}
extension UIApplication {
    class func topViewController(controller: UIViewController? = UIApplication.shared.keyWindow?.rootViewController) -> UIViewController? {
        if let navigationController = controller as? UINavigationController {
            return topViewController(controller: navigationController.visibleViewController)
        }
        else if let tabController = controller as? UITabBarController {
            if let selected = tabController.selectedViewController {
                return topViewController(controller: selected)
            }
        }
        if let presented = controller?.presentedViewController {
            return topViewController(controller: presented)
        }
        return controller
    }
}

