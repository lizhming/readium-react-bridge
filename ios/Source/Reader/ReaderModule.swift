//
//  ReaderModule.swift
//  r2-testapp-swift
//
//  Created by Mickaël Menu on 22.02.19.
//
//  Copyright 2019 European Digital Reading Lab. All rights reserved.
//  Licensed to the Readium Foundation under one or more contributor license agreements.
//  Use of this source code is governed by a BSD-style license which is detailed in the
//  LICENSE file present in the project repository where this source code is maintained.
//

import Foundation
import UIKit
import R2Shared


/// The ReaderModule handles the presentation of publications to be read by the user.
/// It contains sub-modules implementing ReaderFormatModule to handle each format of publication (eg. CBZ, EPUB).
protocol ReaderModuleAPI {
    
    var delegate: ReaderModuleDelegate? { get }
    
    /// Presents the given publication to the user, inside the given navigation controller.
    /// - Parameter completion: Called once the publication is presented, or if an error occured.
    func presentPublication(publication: Publication, book: Book, in navigationController: UINavigationController?, completion: @escaping () -> Void)
    
}

protocol ReaderModuleDelegate: ModuleDelegate {
    
    /// Called when the reader needs to load the R2 DRM object for the given publication.
    func readerLoadDRM(for book: Book, completion: @escaping (CancellableResult<DRM?>) -> Void)
    
}


final class ReaderModule: ReaderModuleAPI {
    
    weak var delegate: ReaderModuleDelegate?
    private let resourcesServer: ResourcesServer
    
    /// Sub-modules to handle different publication formats (eg. EPUB, CBZ)
    var formatModules: [ReaderFormatModule] = []
    
    private let factory = ReaderFactory()
    
    init(delegate: ReaderModuleDelegate?, resourcesServer: ResourcesServer) {
        self.delegate = delegate
        self.resourcesServer = resourcesServer
        
        formatModules = [
            CBZModule(delegate: self),
            EPUBModule(delegate: self),
        ]
        
        if #available(iOS 11.0, *) {
            formatModules.append(PDFModule(delegate: self))
        }
    }
    
    @objc func onClickBack(_ sender: UIBarButtonItem) {
        UIApplication.topViewController()?.dismiss(animated: false, completion: nil)
    }
    func presentPublication(publication: Publication, book: Book, in navigationController: UINavigationController?, completion: @escaping () -> Void) {
        guard let delegate = delegate else {
            fatalError("Reader delegate not set")
        }
        func present(_ viewController: UIViewController) {
          let backItem = UIBarButtonItem();//.init(title: "Back", style: .plain, target: self, action: #selector(onClickBack(_:)))
            backItem.title = "Back"
//            backItem.action = #selector(onClickBack)
            viewController.navigationItem.backBarButtonItem = backItem
//            viewController.navigationItem.leftBarButtonItem = backItem
            viewController.hidesBottomBarWhenPushed = true
//            navigationController.pushViewController(viewController, animated: true)
            let navVC = UINavigationController() //(rootViewController: viewController)
          
//            navVC.navigationItem.leftBarButtonItem
          if let vc = UIApplication.shared.keyWindow?.rootViewController {
            UIApplication.topViewController()?.present(navVC, animated: true, completion: nil)
//            vc.present(viewController, animated: true, completion: nil)
            navVC.pushViewController(viewController, animated: true)
//            vc.present(navVC, animated: true, completion: nil)
            
          }
          
        }
        
        delegate.readerLoadDRM(for: book) { [resourcesServer] result in
            switch result {
            case .failure(let error):
//                delegate.presentError(error, from: navigationController)
                completion()
                
            case .success(let drm):
                guard let module = self.formatModules.first(where:{ $0.publicationFormats.contains(publication.format) }) else {
//                    delegate.presentError(ReaderError.formatNotSupported, from: navigationController)
                    completion()
                    return
                }
                
                do {
                    let readerViewController = try module.makeReaderViewController(for: publication, book: book, drm: drm, resourcesServer: resourcesServer)
                    present(readerViewController)
                } catch {
//                    delegate.presentError(error, from: navigationController)
                }
                
                completion()
                
            case .cancelled:
                completion()
            }
        }
    }
    
}


extension ReaderModule: ReaderFormatModuleDelegate {

    func presentDRM(_ drm: DRM, from viewController: UIViewController) {
        let drmViewController: DRMManagementTableViewController = factory.make(drm: drm, delegate: delegate)
        let backItem = UIBarButtonItem()
        backItem.title = ""
        drmViewController.navigationItem.backBarButtonItem = backItem
        viewController.navigationController?.pushViewController(drmViewController, animated: true)
    }
    
    func presentOutline(of publication: Publication, delegate: OutlineTableViewControllerDelegate?, from viewController: UIViewController) {
        let outlineTableVC: OutlineTableViewController = factory.make(publication: publication)
        outlineTableVC.delegate = delegate
        viewController.present(UINavigationController(rootViewController: outlineTableVC), animated: true)
    }
    
    func presentAlert(_ title: String, message: String, from viewController: UIViewController) {
        delegate?.presentAlert(title, message: message, from: viewController)
    }
    
    func presentError(_ error: Error?, from viewController: UIViewController) {
        delegate?.presentError(error, from: viewController)
    }

}
