//
//  ReaderViewController.swift
//  r2-testapp-swift
//
//  Created by Mickaël Menu on 07.03.19.
//
//  Copyright 2019 European Digital Reading Lab. All rights reserved.
//  Licensed to the Readium Foundation under one or more contributor license agreements.
//  Use of this source code is governed by a BSD-style license which is detailed in the
//  LICENSE file present in the project repository where this source code is maintained.
//

import SafariServices
import UIKit
import R2Navigator
import R2Shared


/// This class is meant to be subclassed by each publication format view controller. It contains the shared behavior, eg. navigation bar toggling.
class ReaderViewController: UIViewController, Loggable {
    
    weak var moduleDelegate: ReaderFormatModuleDelegate?
    
    let navigator: UIViewController & Navigator
    let publication: Publication
    let book: Book
    let drm: DRM?

    lazy var bookmarksDataSource: BookmarkDataSource? = BookmarkDataSource(publicationID: publication.metadata.identifier ?? "")
    
    private(set) var stackView: UIStackView!
    
    init(navigator: UIViewController & Navigator, publication: Publication, book: Book, drm: DRM?) {
        self.navigator = navigator
        self.publication = publication
        self.book = book
        self.drm = drm
        
        super.init(nibName: nil, bundle:  Bundle(path: Bundle.main.path(forResource: "RNReadiumReactBridgeBundle", ofType: "bundle")!))
        
        NotificationCenter.default.addObserver(self, selector: #selector(voiceOverStatusDidChange), name: Notification.Name(UIAccessibilityVoiceOverStatusChanged), object: nil)
    }

    @available(*, unavailable)
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.backgroundColor = .white
      
      navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Back", style: .plain, target: self, action: #selector(onClickBack))
      
        navigationItem.rightBarButtonItems = makeNavigationBarButtons()
        updateNavigationBar(animated: false)
        
        stackView = UIStackView(frame: view.bounds)
        stackView.distribution = .fill
        stackView.axis = .vertical
        view.addSubview(stackView)
        stackView.translatesAutoresizingMaskIntoConstraints = false
        let topConstraint = stackView.topAnchor.constraint(equalTo: view.topAnchor)
        // `accessibilityTopMargin` takes precedence when VoiceOver is enabled.
        topConstraint.priority = .defaultHigh
        NSLayoutConstraint.activate([
            topConstraint,
            stackView.rightAnchor.constraint(equalTo: view.rightAnchor),
            stackView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            stackView.leftAnchor.constraint(equalTo: view.leftAnchor)
        ])

        addChild(navigator)
        stackView.addArrangedSubview(navigator.view)
        navigator.didMove(toParent: self)
        
        stackView.addArrangedSubview(accessibilityToolbar)
    }
    
    override func willMove(toParent parent: UIViewController?) {
        // Restore library's default UI colors
        navigationController?.navigationBar.tintColor = .black
        navigationController?.navigationBar.barTintColor = .white
    }
    
    
    // MARK: - Navigation bar
    
    private var navigationBarHidden: Bool = true {
        didSet {
            updateNavigationBar()
        }
    }
    
    func makeNavigationBarButtons() -> [UIBarButtonItem] {
        var buttons: [UIBarButtonItem] = []
        // Table of Contents
        
        let bundle = Bundle(path: Bundle.main.path(forResource: "RNReadiumReactBridgeBundle", ofType: "bundle")!)!
        
        
        buttons.append(UIBarButtonItem(image: UIImage(named: "menuIcon", in: bundle, compatibleWith: nil), style: .plain, target: self, action: #selector(presentOutline)))
        // DRM management
        if drm != nil {
            buttons.append(UIBarButtonItem(image:  UIImage(named: "drm", in: bundle, compatibleWith: nil), style: .plain, target: self, action: #selector(presentDRMManagement)))
        }
        // Bookmarks
        buttons.append(UIBarButtonItem(image:  UIImage(named: "bookmark", in: bundle, compatibleWith: nil), style: .plain, target: self, action: #selector(bookmarkCurrentPosition)))
        
        return buttons
    }
    
    func toggleNavigationBar() {
        navigationBarHidden = !navigationBarHidden
    }
    
    func updateNavigationBar(animated: Bool = true) {
        let hidden = navigationBarHidden && !UIAccessibility.isVoiceOverRunning
        navigationController?.setNavigationBarHidden(hidden, animated: animated)
        setNeedsStatusBarAppearanceUpdate()
    }
    
    override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation {
        return .slide
    }
    
    override var prefersStatusBarHidden: Bool {
        return navigationBarHidden && !UIAccessibility.isVoiceOverRunning
    }

    
    // MARK: - Locations
    /// FIXME: This should be implemented in a shared Navigator interface, using Locators.
    
    var currentBookmark: Bookmark? {
        fatalError("Not implemented")
    }
  
  @objc func onClickBack(){
    self.dismiss(animated: true, completion: nil)
  }
    // MARK: - Outlines

    @objc func presentOutline() {
        moduleDelegate?.presentOutline(of: publication, delegate: self, from: self)
    }
    
    
    // MARK: - Bookmarks
    
    @objc func bookmarkCurrentPosition() {
        guard let dataSource = bookmarksDataSource,
            let bookmark = currentBookmark,
            dataSource.addBookmark(bookmark: bookmark) else
        {
            toast(NSLocalizedString("reader_bookmark_failure_message", bundle: Bundle(path: Bundle.main.path(forResource: "RNReadiumReactBridgeBundle", ofType: "bundle")!)!, comment: "Error message when adding a new bookmark failed"), on: view, duration: 2)
            return
        }
        toast(NSLocalizedString("reader_bookmark_success_message", bundle: Bundle(path: Bundle.main.path(forResource: "RNReadiumReactBridgeBundle", ofType: "bundle")!)!, comment: "Success message when adding a bookmark"), on: view, duration: 1)
    }
    
    
    // MARK: - DRM
    
    @objc func presentDRMManagement() {
        guard let drm = drm else {
            return
        }
        moduleDelegate?.presentDRM(drm, from: self)
    }
    

    // MARK: - Accessibility
    
    /// Constraint used to shift the content under the navigation bar, since it is always visible when VoiceOver is running.
    private lazy var accessibilityTopMargin: NSLayoutConstraint = {
        let topAnchor: NSLayoutYAxisAnchor = {
            if #available(iOS 11.0, *) {
                return self.view.safeAreaLayoutGuide.topAnchor
            } else {
                return self.topLayoutGuide.bottomAnchor
            }
        }()
        return self.stackView.topAnchor.constraint(equalTo: topAnchor)
    }()
    
    private lazy var accessibilityToolbar: UIToolbar = {
        func makeItem(_ item: UIBarButtonItem.SystemItem, label: String? = nil, action: Selector? = nil) -> UIBarButtonItem {
            let button = UIBarButtonItem(barButtonSystemItem: item, target: (action != nil) ? self : nil, action: action)
            button.accessibilityLabel = label
            return button
        }
        
        let toolbar = UIToolbar(frame: .zero)
        toolbar.items = [
            makeItem(.flexibleSpace),
            makeItem(.rewind, label: NSLocalizedString("reader_backward_a11y_label", bundle: Bundle(path: Bundle.main.path(forResource: "RNReadiumReactBridgeBundle", ofType: "bundle")!)!, comment: "Accessibility label to go backward in the publication"), action: #selector(goBackward)),
            makeItem(.flexibleSpace),
            makeItem(.fastForward, label: NSLocalizedString("reader_forward_a11y_label", bundle: Bundle(path: Bundle.main.path(forResource: "RNReadiumReactBridgeBundle", ofType: "bundle")!)!, comment: "Accessibility label to go forward in the publication"), action: #selector(goForward)),
            makeItem(.flexibleSpace),
        ]
        toolbar.isHidden = !UIAccessibility.isVoiceOverRunning
        toolbar.tintColor = UIColor.black
        return toolbar
    }()
    
    private var isVoiceOverRunning = UIAccessibility.isVoiceOverRunning
    
    @objc private func voiceOverStatusDidChange() {
        let isRunning = UIAccessibility.isVoiceOverRunning
        // Avoids excessive settings refresh when the status didn't change.
        guard isVoiceOverRunning != isRunning else {
            return
        }
        isVoiceOverRunning = isRunning
        accessibilityTopMargin.isActive = isRunning
        accessibilityToolbar.isHidden = !isRunning
        updateNavigationBar()
    }
    
    @objc private func goBackward() {
        navigator.goBackward()
    }
    
    @objc private func goForward() {
        navigator.goForward()
    }
    
}

extension ReaderViewController: NavigatorDelegate {

    func navigator(_ navigator: Navigator, locationDidChange locator: Locator) {
        do {
            try BooksDatabase.shared.books.saveProgression(locator, of: book)
        } catch {
            log(.error, error)
        }
    }
    
    func navigator(_ navigator: Navigator, presentExternalURL url: URL) {
        // SFSafariViewController crashes when given an URL without an HTTP scheme.
        guard ["http", "https"].contains(url.scheme?.lowercased() ?? "") else {
            return
        }
        present(SFSafariViewController(url: url), animated: true)
    }
    
    func navigator(_ navigator: Navigator, presentError error: NavigatorError) {
        moduleDelegate?.presentError(error, from: self)
    }
    
}

extension ReaderViewController: VisualNavigatorDelegate {
    
    func navigator(_ navigator: VisualNavigator, didTapAt point: CGPoint) {
        let viewport = navigator.view.bounds
        // Skips to previous/next pages if the tap is on the content edges.
        let thresholdRange = 0...(0.2 * viewport.width)
        var moved = false
        if thresholdRange ~= point.x {
            moved = navigator.goLeft(animated: false)
        } else if thresholdRange ~= (viewport.maxX - point.x) {
            moved = navigator.goRight(animated: false)
        }
        
        if !moved {
            toggleNavigationBar()
        }
    }
    
}

extension ReaderViewController: OutlineTableViewControllerDelegate {
    
    func outline(_ outlineTableViewController: OutlineTableViewController, goTo location: Locator) {
        navigator.go(to: location)
    }

}
